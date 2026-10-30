package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.medication.internal.domain.DoseCancellationReason;
import com.eldercare.backend.medication.internal.domain.DoseStatus;
import com.eldercare.backend.medication.internal.domain.MedicationDose;
import com.eldercare.backend.medication.internal.domain.MedicationReconciliation;
import com.eldercare.backend.medication.internal.domain.Prescription;
import com.eldercare.backend.medication.internal.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.domain.ReconciliationCorrectionRequest;
import com.eldercare.backend.medication.internal.domain.ReconciliationCorrectionStatus;
import com.eldercare.backend.medication.internal.domain.ReconciliationDecision;
import com.eldercare.backend.medication.internal.domain.ReconciliationItem;
import com.eldercare.backend.medication.internal.domain.ReconciliationItemSource;
import com.eldercare.backend.medication.internal.domain.ReconciliationReason;
import com.eldercare.backend.medication.internal.domain.ReconciliationStatus;
import com.eldercare.backend.medication.internal.domain.SuspensionReason;
import com.eldercare.backend.medication.internal.domain.TerminationReason;
import com.eldercare.backend.medication.internal.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationReconciliationRepository;
import com.eldercare.backend.medication.internal.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.persistence.ReconciliationCorrectionRequestRepository;
import com.eldercare.backend.medication.internal.persistence.ReconciliationItemRepository;
import com.eldercare.backend.medication.internal.persistence.ResidentMedicationLock;
import com.eldercare.backend.medication.internal.service.request.CreatePrescriptionBody;
import com.eldercare.backend.medication.internal.service.request.CreateReconciliationBody;
import com.eldercare.backend.medication.internal.service.request.ProposeCorrectionBody;
import com.eldercare.backend.medication.internal.service.request.ReconciliationItemBody;
import com.eldercare.backend.medication.internal.service.response.PrescriptionResponse;
import com.eldercare.backend.medication.internal.service.response.ReconciliationCorrectionResponse;
import com.eldercare.backend.medication.internal.service.response.ReconciliationItemResponse;
import com.eldercare.backend.medication.internal.service.response.ReconciliationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business service for Medication Reconciliation (FR-034 -> FR-039, BR-M07-09, BR-M07-25, SC-014; research D-13, D-16).
 */
@Service
public class MedicationReconciliationService implements ReconciliationCheckPort {

    private final MedicationReconciliationRepository reconciliationRepo;
    private final ReconciliationItemRepository itemRepo;
    private final ReconciliationCorrectionRequestRepository correctionRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final MedicationDoseRepository doseRepo;
    private final PrescriptionService prescriptionService;
    private final ResidentMedicationLock residentMedicationLock;
    private final ObjectMapper objectMapper;
    private final BusinessTime businessTime;
    private final ParameterApi parameters;

    @Autowired
    public MedicationReconciliationService(
            MedicationReconciliationRepository reconciliationRepo,
            ReconciliationItemRepository itemRepo,
            ReconciliationCorrectionRequestRepository correctionRepo,
            PrescriptionRepository prescriptionRepo,
            @Autowired(required = false) MedicationDoseRepository doseRepo,
            PrescriptionService prescriptionService,
            ResidentMedicationLock residentMedicationLock,
            @Autowired(required = false) ObjectMapper objectMapper,
            BusinessTime businessTime,
            ParameterApi parameters) {
        this.reconciliationRepo = reconciliationRepo;
        this.itemRepo = itemRepo;
        this.correctionRepo = correctionRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.doseRepo = doseRepo;
        this.prescriptionService = prescriptionService;
        this.residentMedicationLock = residentMedicationLock;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.businessTime = businessTime;
        this.parameters = parameters;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOpenOrPendingReconciliation(long residentId) {
        return reconciliationRepo.findOpenOrPendingByResidentId(residentId).isPresent();
    }

    @Transactional
    public ReconciliationResponse create(
            Long residentId, CreateReconciliationBody body, Long currentUserId, boolean isDoctor) {
        residentMedicationLock.lock(residentId);

        // 1. Check whether resident already has an open or pending reconciliation ticket (FR-034)
        if (hasOpenOrPendingReconciliation(residentId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "OPEN_RECONCILIATION_EXISTS",
                    "Resident already has an open or pending confirmation reconciliation ticket (FR-034)");
        }

        // 2. Classify ticket creation reason
        ReconciliationReason reason = parseReason(body.getReason());
        Instant deadlineAt = null;

        if (reason == ReconciliationReason.PROFESSIONAL_REQUEST) {
            // Professional request: reason detail is required (FR-034b)
            if (body.getReasonDetail() == null || body.getReasonDetail().isBlank()) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "INVALID_VALUE",
                        "Reason detail is required for professional request reconciliation (FR-034b)");
            }
            // Do not suspend prescriptions, no deadline (deadlineAt = null)
        } else {
            // Admission or Hospital return: 24-hour deadline (CFG-M07-05)
            deadlineAt = businessTime
                    .now()
                    .plus(parameters.read(ParameterCode.CFG_M07_05).asDuration());

            // Suspend active prescriptions pending reconciliation (FR-034)
            List<Prescription> activePrescriptions =
                    prescriptionRepo.findByResidentIdAndStatusIn(residentId, List.of(PrescriptionStatus.ACTIVE));
            for (Prescription p : activePrescriptions) {
                p.suspend(SuspensionReason.RECONCILIATION_PENDING);
                prescriptionRepo.save(p);
            }
        }

        MedicationReconciliation ticket = new MedicationReconciliation(
                residentId,
                reason,
                body.getReasonDetail(),
                deadlineAt,
                currentUserId,
                businessTime.now(),
                businessTime.now());

        MedicationReconciliation saved = reconciliationRepo.save(ticket);
        return toResponse(saved, List.of());
    }

    @Transactional(readOnly = true)
    public ReconciliationResponse get(Long id) {
        MedicationReconciliation ticket = reconciliationRepo.findById(id).orElseThrow(NotFoundException::new);
        List<ReconciliationItem> items = itemRepo.findByReconciliationIdOrderByItemOrderAsc(id);
        return toResponse(ticket, items);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationResponse> listByResident(Long residentId) {
        List<MedicationReconciliation> tickets = reconciliationRepo.findByResidentId(residentId);
        return tickets.stream()
                .map(t -> {
                    List<ReconciliationItem> items = itemRepo.findByReconciliationIdOrderByItemOrderAsc(t.getId());
                    return toResponse(t, items);
                })
                .toList();
    }

    @Transactional
    public ReconciliationResponse updateItems(
            Long id, String ifMatch, List<ReconciliationItemBody> itemsBody, Long currentUserId) {
        MedicationReconciliation ticket = reconciliationRepo.findById(id).orElseThrow(NotFoundException::new);
        residentMedicationLock.lock(ticket.getResidentId());

        if (ticket.getStatus() != ReconciliationStatus.OPEN) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_STATE",
                    "Items can only be updated when reconciliation sheet is in OPEN status (FR-035)");
        }

        if (ifMatch != null) {
            VersionEtag.requireMatch(ifMatch, ticket.getVersion(), toResponse(ticket, List.of()));
        }

        itemRepo.deleteByReconciliationId(id);

        List<ReconciliationItem> savedItems = new ArrayList<>();
        if (itemsBody != null) {
            int order = 1;
            for (ReconciliationItemBody b : itemsBody) {
                ReconciliationItemSource source = parseSource(b.getSource());
                ReconciliationDecision decision = b.getDecision() != null ? parseDecision(b.getDecision()) : null;

                String newPrescriptionJson = null;
                if (b.getNewPrescription() != null) {
                    try {
                        newPrescriptionJson = objectMapper.writeValueAsString(b.getNewPrescription());
                    } catch (JsonProcessingException e) {
                        throw new BusinessException(
                                HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "New prescription data is invalid");
                    }
                }

                ReconciliationItem item = new ReconciliationItem(
                        id,
                        order++,
                        source,
                        b.getPrescriptionId(),
                        b.getAdmissionMedicationItemId(),
                        b.getMedicationDescription(),
                        decision != null ? decision : ReconciliationDecision.CONTINUE,
                        b.getReason(),
                        newPrescriptionJson,
                        currentUserId,
                        businessTime.now(),
                        businessTime.now());

                savedItems.add(itemRepo.save(item));
            }
        }

        return toResponse(ticket, savedItems);
    }

    @Transactional
    public ReconciliationResponse submit(Long id, Long currentUserId) {
        MedicationReconciliation ticket = reconciliationRepo.findById(id).orElseThrow(NotFoundException::new);
        residentMedicationLock.lock(ticket.getResidentId());

        if (ticket.getStatus() != ReconciliationStatus.OPEN) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_STATE",
                    "Reconciliation sheet is not in OPEN status to submit for confirmation");
        }

        List<ReconciliationItem> items = itemRepo.findByReconciliationIdOrderByItemOrderAsc(id);
        if (items.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "MISSING_ITEMS", "Reconciliation sheet has no items (FR-034)");
        }

        // Verify item completeness (FR-034, FR-034a, BR-M07-25)
        for (ReconciliationItem item : items) {
            if (item.getDecision() == null) {
                throw new BusinessException(HttpStatus.CONFLICT, "MISSING_ITEMS", "Item missing decision (FR-034)");
            }
            if (item.getDecision() == ReconciliationDecision.DISCONTINUE
                    && (item.getReason() == null || item.getReason().isBlank())) {
                throw new BusinessException(
                        HttpStatus.CONFLICT, "MISSING_ITEMS", "Discontinue item missing reason (FR-034)");
            }
        }

        // Kiểm tra độ bao phủ các đơn hiện tại của người cao tuổi (BR-M07-25, FR-034)
        List<Prescription> currentPrescriptions = prescriptionRepo.findByResidentIdAndStatusIn(
                ticket.getResidentId(),
                List.of(PrescriptionStatus.ACTIVE, PrescriptionStatus.SUSPENDED, PrescriptionStatus.PENDING_EFFECTIVE));

        Set<Long> itemPrescriptionIds = items.stream()
                .map(ReconciliationItem::getPrescriptionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Prescription p : currentPrescriptions) {
            if (!itemPrescriptionIds.contains(p.getId())) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "MISSING_ITEMS",
                        "Thiếu dòng đối chiếu cho đơn thuốc ID=" + p.getId()
                                + " của người cao tuổi (FR-034, BR-M07-25)");
            }
        }

        ticket.submit(currentUserId, businessTime.now());
        MedicationReconciliation saved = reconciliationRepo.save(ticket);
        return toResponse(saved, items);
    }

    @Transactional
    public ReconciliationResponse returnForEdit(Long id, String reason, Long currentUserId) {
        MedicationReconciliation ticket = reconciliationRepo.findById(id).orElseThrow(NotFoundException::new);
        residentMedicationLock.lock(ticket.getResidentId());

        if (ticket.getStatus() != ReconciliationStatus.PENDING_CONFIRMATION) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "INVALID_STATE", "Chỉ có thể trả lại phiếu đang chờ xác nhận (FR-035)");
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "Lý do trả lại bắt buộc (FR-035)");
        }

        ticket.returnForEdit(reason);
        MedicationReconciliation saved = reconciliationRepo.save(ticket);
        List<ReconciliationItem> items = itemRepo.findByReconciliationIdOrderByItemOrderAsc(id);
        return toResponse(saved, items);
    }

    @Transactional
    public ReconciliationResponse confirm(Long id, Long currentUserId, boolean isDoctor) {
        MedicationReconciliation ticket = reconciliationRepo.findById(id).orElseThrow(NotFoundException::new);
        residentMedicationLock.lock(ticket.getResidentId());

        if (ticket.getStatus() != ReconciliationStatus.PENDING_CONFIRMATION) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "INVALID_STATE", "Phiếu không ở trạng thái Chờ xác nhận (FR-036)");
        }

        Long actualUserId = currentUserId;

        // Quy tắc hai người (SC-014, BR-M07-09, Q-55):
        // Người xác nhận phải khác người lập và người gửi
        if (Objects.equals(ticket.getSubmittedById(), actualUserId)
                || Objects.equals(ticket.getCreatedById(), actualUserId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "TWO_PERSON_RULE",
                    "Người xác nhận không được trùng với người lập hoặc gửi phiếu (SC-014, BR-M07-09)");
        }

        List<ReconciliationItem> items = itemRepo.findByReconciliationIdOrderByItemOrderAsc(id);

        // Người xác nhận không được là người đã ghi dòng
        for (ReconciliationItem item : items) {
            if (Objects.equals(item.getCreatedById(), actualUserId)) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "TWO_PERSON_RULE",
                        "Người xác nhận không được trùng với người đã ghi dòng đối chiếu (SC-014, BR-M07-09)");
            }
        }

        // Kiểm tra thẩm quyền nếu có tạo đơn nội bộ
        for (ReconciliationItem item : items) {
            if (item.getDecision() == ReconciliationDecision.ADD_NEW
                    || item.getDecision() == ReconciliationDecision.CHANGE_DOSE) {
                if (item.getNewPrescriptionJson() != null) {
                    try {
                        CreatePrescriptionBody newPrescription =
                                objectMapper.readValue(item.getNewPrescriptionJson(), CreatePrescriptionBody.class);
                        if (newPrescription.getPrescriber() != null
                                && !"EXTERNAL"
                                        .equalsIgnoreCase(
                                                newPrescription.getPrescriber().getType())
                                && !isDoctor) {
                            throw new BusinessException(
                                    HttpStatus.FORBIDDEN,
                                    "PRESCRIBING_DOCTOR_REQUIRED",
                                    "Cần bác sĩ có quyền kê đơn để xác nhận phiếu có tạo đơn nội bộ (BR-M07-09)");
                        }
                    } catch (JsonProcessingException e) {
                        // ignore parse exception here
                    }
                }
            }
        }

        // Áp dụng quyết định từng dòng trong cùng giao dịch (FR-036)
        Instant now = businessTime.now();
        for (ReconciliationItem item : items) {
            if (item.getDecision() == ReconciliationDecision.CONTINUE) {
                if (item.getPrescriptionId() != null) {
                    prescriptionRepo.findById(item.getPrescriptionId()).ifPresent(p -> {
                        if (p.getStatus() == PrescriptionStatus.SUSPENDED) {
                            p.resume();
                            prescriptionRepo.save(p);
                        }
                    });
                }
            } else if (item.getDecision() == ReconciliationDecision.DISCONTINUE) {
                if (item.getPrescriptionId() != null) {
                    prescriptionRepo.findById(item.getPrescriptionId()).ifPresent(p -> {
                        if (p.getStatus() == PrescriptionStatus.ACTIVE
                                || p.getStatus() == PrescriptionStatus.SUSPENDED
                                || p.getStatus() == PrescriptionStatus.PENDING_EFFECTIVE) {
                            p.discontinue(TerminationReason.DISCONTINUED);
                            prescriptionRepo.save(p);

                            // Hủy các liều chưa đóng
                            if (doseRepo != null) {
                                List<MedicationDose> openDoses = doseRepo.findByPrescriptionIdAndStatusIn(
                                        p.getId(),
                                        List.of(
                                                DoseStatus.UPCOMING,
                                                DoseStatus.DUE,
                                                DoseStatus.LATE,
                                                DoseStatus.SUSPENDED));
                                for (MedicationDose d : openDoses) {
                                    d.cancel(DoseCancellationReason.DISCONTINUED, now);
                                    doseRepo.save(d);
                                }
                            }
                        }
                    });
                }
            } else if (item.getDecision() == ReconciliationDecision.CHANGE_DOSE) {
                // Ngừng đơn cũ
                if (item.getPrescriptionId() != null) {
                    prescriptionRepo.findById(item.getPrescriptionId()).ifPresent(p -> {
                        p.discontinue(TerminationReason.DISCONTINUED);
                        prescriptionRepo.save(p);
                    });
                }
                // Tạo đơn thay thế
                if (item.getNewPrescriptionJson() != null) {
                    try {
                        CreatePrescriptionBody newPrescription =
                                objectMapper.readValue(item.getNewPrescriptionJson(), CreatePrescriptionBody.class);
                        PrescriptionResponse created = prescriptionService.create(
                                PrescriptionService.target(ticket.getResidentId()),
                                ticket.getResidentId(),
                                newPrescription,
                                actualUserId,
                                isDoctor);
                        item.recordCreatedPrescription(created.id());
                        itemRepo.save(item);
                    } catch (JsonProcessingException e) {
                        throw new BusinessException(
                                HttpStatus.INTERNAL_SERVER_ERROR, "ERROR", "Không thể xử lý dữ liệu đơn mới");
                    }
                }
            } else if (item.getDecision() == ReconciliationDecision.ADD_NEW) {
                if (item.getNewPrescriptionJson() != null) {
                    try {
                        CreatePrescriptionBody newPrescription =
                                objectMapper.readValue(item.getNewPrescriptionJson(), CreatePrescriptionBody.class);
                        PrescriptionResponse created = prescriptionService.create(
                                PrescriptionService.target(ticket.getResidentId()),
                                ticket.getResidentId(),
                                newPrescription,
                                actualUserId,
                                isDoctor);
                        item.recordCreatedPrescription(created.id());
                        itemRepo.save(item);
                    } catch (JsonProcessingException e) {
                        throw new BusinessException(
                                HttpStatus.INTERNAL_SERVER_ERROR, "ERROR", "Không thể xử lý dữ liệu đơn mới");
                    }
                }
            }
        }

        ticket.confirm(actualUserId, now);
        MedicationReconciliation saved = reconciliationRepo.save(ticket);
        return toResponse(saved, items);
    }

    @Transactional
    public ReconciliationCorrectionResponse proposeCorrection(Long id, ProposeCorrectionBody body, Long currentUserId) {
        MedicationReconciliation ticket = reconciliationRepo.findById(id).orElseThrow(NotFoundException::new);

        if (ticket.getStatus() != ReconciliationStatus.CONFIRMED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "INVALID_STATE", "Chỉ có thể đề nghị đính chính phiếu đã xác nhận (FR-039)");
        }

        Long actualUserId = currentUserId;

        // Chỉ người lập, người gửi hoặc người xác nhận phiếu mới được đề nghị đính chính
        boolean isParticipant = Objects.equals(ticket.getCreatedById(), actualUserId)
                || Objects.equals(ticket.getSubmittedById(), actualUserId)
                || Objects.equals(ticket.getConfirmedById(), actualUserId);

        if (!isParticipant) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "Chỉ người liên quan phiếu mới được đề nghị đính chính (BR-M07-17)");
        }

        if (body.getReason() == null || body.getReason().isBlank()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "Lý do đính chính là bắt buộc (FR-039)");
        }

        String jsonContent;
        try {
            jsonContent = objectMapper.writeValueAsString(body.getCorrectedContent());
        } catch (JsonProcessingException e) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "Nội dung đúng không hợp lệ");
        }

        ReconciliationCorrectionRequest req = new ReconciliationCorrectionRequest(
                id, jsonContent, body.getReason(), actualUserId, businessTime.now(), businessTime.now());
        ReconciliationCorrectionRequest saved = correctionRepo.save(req);

        ReconciliationCorrectionResponse res = new ReconciliationCorrectionResponse();
        res.setId(saved.getId());
        res.setReconciliationId(saved.getReconciliationId());
        res.setCorrectedContent(body.getCorrectedContent());
        res.setReason(saved.getReason());
        res.setStatus(saved.getStatus().name());
        res.setCreatedById(saved.getCreatedById());
        res.setCreatedAt(saved.getCreatedAt());
        return res;
    }

    @Transactional
    public ReconciliationCorrectionResponse confirmCorrection(Long correctionId, Long currentUserId) {
        ReconciliationCorrectionRequest req =
                correctionRepo.findById(correctionId).orElseThrow(NotFoundException::new);

        if (req.getStatus() != ReconciliationCorrectionStatus.PENDING_CONFIRMATION) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "INVALID_STATE", "Đề nghị đính chính không ở trạng thái Chờ xác nhận");
        }

        Long actualUserId = currentUserId;
        if (Objects.equals(req.getCreatedById(), actualUserId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "TWO_PERSON_RULE",
                    "Người xác nhận đính chính phải khác người lập đề nghị (BR-M07-17)");
        }

        req.confirm(actualUserId, businessTime.now(), correctionId);
        ReconciliationCorrectionRequest saved = correctionRepo.save(req);

        ReconciliationCorrectionResponse res = new ReconciliationCorrectionResponse();
        res.setId(saved.getId());
        res.setReconciliationId(saved.getReconciliationId());
        res.setReason(saved.getReason());
        res.setStatus(saved.getStatus().name());
        res.setCreatedById(saved.getCreatedById());
        res.setCreatedAt(saved.getCreatedAt());
        res.setConfirmedById(saved.getConfirmedById());
        res.setConfirmedAt(saved.getConfirmedAt());
        return res;
    }

    @Transactional
    public ReconciliationCorrectionResponse rejectCorrection(Long correctionId, String reason, Long currentUserId) {
        ReconciliationCorrectionRequest req =
                correctionRepo.findById(correctionId).orElseThrow(NotFoundException::new);

        if (req.getStatus() != ReconciliationCorrectionStatus.PENDING_CONFIRMATION) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "INVALID_STATE", "Đề nghị đính chính không ở trạng thái Chờ xác nhận");
        }

        Long actualUserId = currentUserId;
        req.reject(actualUserId, businessTime.now(), reason);
        ReconciliationCorrectionRequest saved = correctionRepo.save(req);

        ReconciliationCorrectionResponse res = new ReconciliationCorrectionResponse();
        res.setId(saved.getId());
        res.setReconciliationId(saved.getReconciliationId());
        res.setReason(saved.getReason());
        res.setStatus(saved.getStatus().name());
        res.setCreatedById(saved.getCreatedById());
        res.setCreatedAt(saved.getCreatedAt());
        res.setConfirmedById(saved.getConfirmedById());
        res.setConfirmedAt(saved.getConfirmedAt());
        return res;
    }

    // --- Helpers ---

    private ReconciliationReason parseReason(String reasonStr) {
        if (reasonStr == null) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "Lý do đối chiếu không được để trống");
        }
        return switch (reasonStr.toUpperCase()) {
            case "TIEP_NHAN", "ADMISSION" -> ReconciliationReason.ADMISSION;
            case "TRO_VE_BENH_VIEN", "HOSPITAL_RETURN" -> ReconciliationReason.HOSPITAL_RETURN;
            case "THEO_YEU_CAU_CHUYEN_MON", "PROFESSIONAL_REQUEST" -> ReconciliationReason.PROFESSIONAL_REQUEST;
            default ->
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "Lý do không hợp lệ: " + reasonStr);
        };
    }

    private ReconciliationItemSource parseSource(String sourceStr) {
        if (sourceStr == null) {
            return ReconciliationItemSource.CURRENT_PRESCRIPTION;
        }
        return switch (sourceStr.toUpperCase()) {
            case "DON_HIEN_CO", "CURRENT_PRESCRIPTION", "EXISTING_PRESCRIPTION" ->
                ReconciliationItemSource.CURRENT_PRESCRIPTION;
            case "DON_RA_VIEN", "DISCHARGE_PRESCRIPTION" -> ReconciliationItemSource.DISCHARGE_PRESCRIPTION;
            case "THUOC_DANG_DUNG_TIEP_NHAN", "ADMISSION_CURRENT_MEDICATION", "ADMISSION_MEDICATION" ->
                ReconciliationItemSource.ADMISSION_CURRENT_MEDICATION;
            default -> ReconciliationItemSource.CURRENT_PRESCRIPTION;
        };
    }

    private ReconciliationDecision parseDecision(String decisionStr) {
        return switch (decisionStr.toUpperCase()) {
            case "TIEP_TUC", "CONTINUE" -> ReconciliationDecision.CONTINUE;
            case "NGUNG", "DISCONTINUE" -> ReconciliationDecision.DISCONTINUE;
            case "THAY_DOI_LIEU", "CHANGE_DOSE", "CHANGE_DOSAGE" -> ReconciliationDecision.CHANGE_DOSE;
            case "THEM_MOI", "ADD_NEW" -> ReconciliationDecision.ADD_NEW;
            default ->
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "Quyết định không hợp lệ: " + decisionStr);
        };
    }

    private ReconciliationResponse toResponse(MedicationReconciliation t, List<ReconciliationItem> items) {
        ReconciliationResponse res = new ReconciliationResponse();
        res.setId(t.getId());
        res.setResidentId(t.getResidentId());
        res.setReason(t.getReason().name());
        res.setReasonDetail(t.getReasonDetail());
        res.setStatus(t.getStatus().name());
        res.setDeadlineAt(t.getDeadlineAt());
        res.setOverdue(t.getOverdueAt() != null
                || (t.getDeadlineAt() != null && t.getDeadlineAt().isBefore(businessTime.now())));
        res.setCreatedBy(String.valueOf(t.getCreatedById()));
        if (t.getSubmittedById() != null) {
            res.setSubmittedBy(String.valueOf(t.getSubmittedById()));
        }
        if (t.getConfirmedById() != null) {
            res.setConfirmedBy(String.valueOf(t.getConfirmedById()));
        }
        res.setReturnReason(t.getReturnReason());
        res.setVersion(t.getVersion());

        List<ReconciliationItemResponse> itemResponses = new ArrayList<>();
        if (items != null) {
            for (ReconciliationItem item : items) {
                ReconciliationItemResponse ir = new ReconciliationItemResponse();
                ir.setId(item.getId());
                ir.setSource(item.getSource().name());
                ir.setPrescriptionId(item.getPrescriptionId());
                ir.setAdmissionMedicationItemId(item.getAdmissionMedicationItemId());
                ir.setMedicationDescription(item.getMedicationDescription());
                ir.setDecision(item.getDecision() != null ? item.getDecision().name() : null);
                ir.setReason(item.getReason());
                ir.setDiscontinuedOutsideTicket(item.isDiscontinuedOutsideTicket());
                ir.setCreatedPrescriptionId(item.getCreatedPrescriptionId());

                if (item.getNewPrescriptionJson() != null) {
                    try {
                        ir.setNewPrescription(
                                objectMapper.readValue(item.getNewPrescriptionJson(), CreatePrescriptionBody.class));
                    } catch (JsonProcessingException ignored) {
                    }
                }
                itemResponses.add(ir);
            }
        }
        res.setItems(itemResponses);

        List<String> actions = new ArrayList<>();
        if (t.getStatus() == ReconciliationStatus.OPEN) {
            actions.add("CAP_NHAT_DONG");
            actions.add("GUI_XAC_NHAN");
        } else if (t.getStatus() == ReconciliationStatus.PENDING_CONFIRMATION) {
            actions.add("TRA_LAI");
            actions.add("XAC_NHAN");
        } else if (t.getStatus() == ReconciliationStatus.CONFIRMED) {
            actions.add("DE_NGHI_DINH_CHINH");
        }
        res.setPermittedActions(actions);

        return res;
    }
}
