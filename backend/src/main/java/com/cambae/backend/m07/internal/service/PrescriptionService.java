package com.cambae.backend.m07.internal.service;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m07.api.PrescriptionApi;
import com.cambae.backend.m07.api.dto.ActiveMedicationView;
import com.cambae.backend.m07.api.dto.MedicationSource;
import com.cambae.backend.m07.api.dto.MedicationSummary;
import com.cambae.backend.m07.api.dto.PrescriptionView;
import com.cambae.backend.m07.internal.domain.DoseCancellationReason;
import com.cambae.backend.m07.internal.domain.DoseStatus;
import com.cambae.backend.m07.internal.domain.FrequencyType;
import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.domain.MedicationDose;
import com.cambae.backend.m07.internal.domain.PrescriberType;
import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.domain.PrescriptionBasis;
import com.cambae.backend.m07.internal.domain.PrescriptionBasisCommand;
import com.cambae.backend.m07.internal.domain.PrescriptionBasisType;
import com.cambae.backend.m07.internal.domain.PrescriptionIssue;
import com.cambae.backend.m07.internal.domain.PrescriptionStatus;
import com.cambae.backend.m07.internal.domain.PrescriptionType;
import com.cambae.backend.m07.internal.domain.SuspensionReason;
import com.cambae.backend.m07.internal.domain.TerminationReason;
import com.cambae.backend.m07.internal.persistence.MedicationDoseRepository;
import com.cambae.backend.m07.internal.persistence.MedicationRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionBasisRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionIssueRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionRepository;
import com.cambae.backend.m07.internal.persistence.ResidentMedicationLock;
import com.cambae.backend.m07.internal.service.request.ChangeDosageBody;
import com.cambae.backend.m07.internal.service.request.DiscontinuePrescriptionBody;
import com.cambae.backend.m07.internal.service.request.ExtendPrescriptionBody;
import com.cambae.backend.m07.internal.service.request.ExternalBasisBody;
import com.cambae.backend.m07.internal.service.request.OrderPrescriptionBody;
import com.cambae.backend.m07.internal.service.response.PrescriptionIssueResponse;
import com.cambae.backend.m07.internal.service.response.PrescriptionResponse;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Đơn thuốc (FR-004 -> FR-017, BR-M07-05 -> BR-M07-08; research D-05).
 */
@Service
public class PrescriptionService implements PrescriptionApi {

    static final String ENTITY_TYPE = "M07.PRESCRIPTION";

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionIssueRepository prescriptionIssueRepository;
    private final PrescriptionBasisRepository prescriptionBasisRepository;
    private final MedicationRepository medicationRepository;
    private final MedicationDoseRepository doseRepository;
    private final ResidentMedicationLock residentMedicationLock;
    private final PrescriptionValidator prescriptionValidator;
    private final HealthAllergyPort healthAllergyPort;
    private final ReconciliationCheckPort reconciliationCheckPort;
    private final BusinessTime businessTime;
    private final ParameterApi parameters;

    @Autowired
    public PrescriptionService(
            PrescriptionRepository prescriptionRepository,
            PrescriptionIssueRepository prescriptionIssueRepository,
            PrescriptionBasisRepository prescriptionBasisRepository,
            MedicationRepository medicationRepository,
            @Autowired(required = false) MedicationDoseRepository doseRepository,
            ResidentMedicationLock residentMedicationLock,
            PrescriptionValidator prescriptionValidator,
            HealthAllergyPort healthAllergyPort,
            @org.springframework.context.annotation.Lazy @Autowired(required = false)
                    ReconciliationCheckPort reconciliationCheckPort,
            BusinessTime businessTime,
            ParameterApi parameters) {
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionIssueRepository = prescriptionIssueRepository;
        this.prescriptionBasisRepository = prescriptionBasisRepository;
        this.medicationRepository = medicationRepository;
        this.doseRepository = doseRepository;
        this.residentMedicationLock = residentMedicationLock;
        this.prescriptionValidator = prescriptionValidator;
        this.healthAllergyPort = healthAllergyPort;
        this.reconciliationCheckPort = reconciliationCheckPort != null ? reconciliationCheckPort : r -> false;
        this.businessTime = businessTime;
        this.parameters = parameters;
    }

    public record CreatePrescriptionCommand(
            Long medicationId,
            BigDecimal dosage,
            String dosageUnit,
            String route,
            PrescriptionType prescriptionType,
            FrequencyType frequencyType,
            String timedSlots,
            Integer everyNHours,
            Instant firstSlot,
            String daysOfWeek,
            Integer minIntervalMinutes,
            Integer maxDosesPer24h,
            Instant startedAt,
            LocalDate endedAt,
            MedicationSource medicationSource,
            PrescriberType prescriberType,
            String facilityName,
            String prescriberName,
            Long prescriberId,
            LocalDate prescribedAt,
            Long prescriptionFileId,
            String instructionNote,
            boolean pendingAdmission,
            Map<String, String> confirmedIssues,
            boolean doctorLicensed) {}

    /** Đối tượng kiểm quyền, phạm vi của lệnh đơn thuốc trên một người cao tuổi (kiểm ở {@code RequiresPermissionAspect}). */
    public static Target target(long residentId) {
        return new Target(ENTITY_TYPE, null, residentId, null);
    }

    /** Đối tượng kiểm quyền của một đơn: người cao tuổi của đơn; không có đơn thì 404. */
    @Transactional(readOnly = true)
    public Target targetOf(long prescriptionId) {
        Prescription p = prescriptionRepository.findById(prescriptionId).orElseThrow(NotFoundException::new);
        return new Target(ENTITY_TYPE, String.valueOf(prescriptionId), p.getResidentId(), null);
    }

    @RequiresPermission(function = Function.PRESCRIPTION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> listByResident(
            Target target, long residentId, Collection<PrescriptionStatus> statuses) {
        List<Prescription> list = (statuses == null || statuses.isEmpty())
                ? prescriptionRepository.findByResidentId(residentId)
                : prescriptionRepository.findByResidentIdAndStatusIn(residentId, statuses);

        return list.stream().map(this::toResponse).toList();
    }

    @RequiresPermission(function = Function.PRESCRIPTION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> listByResident(Target target, long residentId, List<String> statusStrings) {
        Collection<PrescriptionStatus> statuses = null;
        if (statusStrings != null && !statusStrings.isEmpty()) {
            statuses = statusStrings.stream()
                    .map(s -> {
                        try {
                            return PrescriptionStatus.valueOf(s.toUpperCase());
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
        return listByResident(target, residentId, statuses);
    }

    @RequiresPermission(function = Function.PRESCRIPTION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public PrescriptionResponse getById(Target target, long id) {
        Prescription p = prescriptionRepository.findById(id).orElseThrow(NotFoundException::new);
        return toResponse(p);
    }

    @RequiresPermission(function = Function.PRESCRIPTION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public PrescriptionCheckResult check(Target target, long residentId, CreatePrescriptionCommand cmd) {
        return check(target, residentId, cmd, null);
    }

    @RequiresPermission(function = Function.PRESCRIPTION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public PrescriptionCheckResult check(
            Target target, long residentId, CreatePrescriptionCommand cmd, Long replacedPrescriptionId) {
        Medication med = medicationRepository.findById(cmd.medicationId()).orElseThrow(NotFoundException::new);
        List<Prescription> existing = prescriptionRepository.findUnendedByResidentId(residentId);
        List<AllergyItemView> allergies = healthAllergyPort.getActiveAllergies(residentId);

        return prescriptionValidator.validate(med.getAllergenIds(), existing, allergies, replacedPrescriptionId);
    }

    @RequiresPermission(function = Function.PRESCRIPTION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public PrescriptionCheckResult check(
            Target target,
            long residentId,
            com.cambae.backend.m07.internal.service.request.CreatePrescriptionBody body) {
        return check(target, residentId, toCommand(body, 1L, true));
    }

    public CreatePrescriptionCommand toCommand(
            com.cambae.backend.m07.internal.service.request.CreatePrescriptionBody body,
            Long currentUserId,
            boolean isDoctor) {
        FrequencyType freqType = null;
        if (body.getFrequency() != null && body.getFrequency().getType() != null) {
            String type = body.getFrequency().getType().toUpperCase();
            if ("CAC_MOC".equals(type) || "TIMED_SLOTS".equals(type)) {
                freqType = FrequencyType.TIMED_SLOTS;
            } else if ("MOI_N_GIO".equals(type) || "EVERY_N_HOURS".equals(type)) {
                freqType = FrequencyType.EVERY_N_HOURS;
            } else if ("NGAY_TRONG_TUAN".equals(type) || "DAYS_OF_WEEK".equals(type)) {
                freqType = FrequencyType.DAYS_OF_WEEK;
            }
        }

        String timedSlots = null;
        Integer everyNHours = null;
        Instant firstSlot = null;
        String daysOfWeek = null;

        if (body.getFrequency() != null) {
            if (body.getFrequency().getTimedSlots() != null) {
                try {
                    timedSlots = new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(body.getFrequency().getTimedSlots());
                } catch (Exception ignored) {
                }
            }
            everyNHours = body.getFrequency().getEveryNHours();
            firstSlot = body.getFrequency().getFirstSlot();
            if (body.getFrequency().getDaysOfWeek() != null) {
                daysOfWeek = String.join(
                        ",",
                        body.getFrequency().getDaysOfWeek().stream()
                                .map(String::valueOf)
                                .toList());
            }
        }

        PrescriptionType prescriptionType = PrescriptionType.PERIODIC;
        if (body.getPrescriptionType() != null) {
            String loai = body.getPrescriptionType().toUpperCase();
            if ("PRN".equals(loai)) {
                prescriptionType = PrescriptionType.PRN;
            }
        }

        MedicationSource medSource = MedicationSource.FACILITY;
        if (body.getMedicationSource() != null) {
            String src = body.getMedicationSource().toUpperCase();
            if ("GIA_DINH_GUI".equals(src) || "FAMILY_SENT".equals(src)) {
                medSource = MedicationSource.FAMILY_SENT;
            }
        }

        PrescriberType prescriberType = PrescriberType.INTERNAL;
        String facilityName = null;
        String prescriberName = null;
        Long prescriberId = currentUserId;
        LocalDate prescribedAt = businessTime.today();
        Long prescriptionFileId = null;

        if (body.getPrescriber() != null) {
            if ("EXTERNAL".equalsIgnoreCase(body.getPrescriber().getType())
                    || "BEN_NGOAI".equalsIgnoreCase(body.getPrescriber().getType())) {
                prescriberType = PrescriberType.EXTERNAL;
            }
            facilityName = body.getPrescriber().getFacilityName();
            prescriberName = body.getPrescriber().getPrescriberName();
            if (body.getPrescriber().getPrescriberId() != null) {
                prescriberId = body.getPrescriber().getPrescriberId();
            }
            prescribedAt = body.getPrescriber().getPrescribedAt();
            prescriptionFileId = body.getPrescriber().getPrescriptionFileId();
        }

        Map<String, String> confirmedMap = new java.util.HashMap<>();
        if (body.getConfirmedIssues() != null) {
            for (var item : body.getConfirmedIssues()) {
                if (item.getIssueCode() != null && item.getReason() != null) {
                    confirmedMap.put(item.getIssueCode(), item.getReason());
                }
            }
        }

        // Đơn vị liều là đơn vị dùng của thuốc trong danh mục (api-006 NewPrescription không có trường đơn vị)
        String unit = body.getDosageUnit() != null && !body.getDosageUnit().isBlank()
                ? body.getDosageUnit()
                : medicationRepository
                        .findById(body.getMedicationId())
                        .map(Medication::getUsageUnit)
                        .orElse(null);

        return new CreatePrescriptionCommand(
                body.getMedicationId(),
                body.getDosage(),
                unit,
                body.getRoute(),
                prescriptionType,
                freqType,
                timedSlots,
                everyNHours,
                firstSlot,
                daysOfWeek,
                body.getMinIntervalMinutes(),
                body.getMaxDosesPer24h(),
                body.getStartedAt(),
                body.getEndedAt(),
                medSource,
                prescriberType,
                facilityName,
                prescriberName,
                prescriberId,
                prescribedAt,
                prescriptionFileId,
                body.getInstructionNote(),
                body.isPendingAdmission(),
                confirmedMap,
                isDoctor);
    }

    @RequiresPermission(function = Function.PRESCRIPTION, operation = Operation.CREATE)
    @Transactional
    public PrescriptionResponse create(
            Target target,
            long residentId,
            com.cambae.backend.m07.internal.service.request.CreatePrescriptionBody body,
            Long currentUserId,
            boolean isDoctor) {
        return create(target, residentId, toCommand(body, currentUserId, isDoctor));
    }

    @RequiresPermission(function = Function.PRESCRIPTION, operation = Operation.CREATE)
    @Transactional
    public PrescriptionResponse create(Target target, long residentId, CreatePrescriptionCommand cmd) {
        // 1. Khóa theo người trên resident_medication (research D-03)
        residentMedicationLock.lock(residentId);

        // 2. Chặn nếu đang có phiếu đối chiếu Mở hoặc Chờ xác nhận (FR-037)
        if (reconciliationCheckPort.hasOpenOrPendingReconciliation(residentId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RECONCILIATION_IN_PROGRESS",
                    "Người cao tuổi đang có phiếu đối chiếu thuốc chưa hoàn tất (FR-037)");
        }

        // 3. Kiểm tra thẩm quyền kê đơn (BR-M07-05, BR-M06-05)
        if (cmd.prescriberType() == PrescriberType.INTERNAL) {
            if (!cmd.doctorLicensed()) {
                throw new BusinessException(
                        HttpStatus.FORBIDDEN,
                        "LEGAL_CONDITION_NOT_MET",
                        "Chỉ Bác sĩ có giấy phép hành nghề hợp lệ mới được kê đơn nội bộ (BR-M07-05, BR-M06-05)");
            }
        } else {
            // Đơn bên ngoài bắt buộc có tên cơ sở, người kê và ngày kê (BR-M07-05)
            if (cmd.facilityName() == null
                    || cmd.facilityName().isBlank()
                    || cmd.prescriberName() == null
                    || cmd.prescriberName().isBlank()
                    || cmd.prescribedAt() == null) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "INVALID_VALUE",
                        "Đơn bên ngoài bắt buộc phải có thông tin cơ sở kê, người kê và ngày kê (BR-M07-05)");
            }
        }

        // 4. Kiểm tra thuốc trong danh mục
        Medication med = medicationRepository.findById(cmd.medicationId()).orElseThrow(NotFoundException::new);
        if (!med.isActive()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("MEDICATION_INACTIVE", "Thuốc đã ngừng hiệu lực trong danh mục")));
        }

        // 5. Kiểm tra loại đơn và tần suất (FR-004)
        if (cmd.prescriptionType() == PrescriptionType.PERIODIC) {
            if (cmd.frequencyType() == null) {
                throw new ConditionNotMetException(List.of(
                        new UnmetCondition("FREQUENCY_REQUIRED", "Đơn định kỳ bắt buộc phải có kiểu tần suất")));
            }
        } else if (cmd.prescriptionType() == PrescriptionType.PRN) {
            if (cmd.minIntervalMinutes() == null
                    || cmd.minIntervalMinutes() <= 0
                    || cmd.maxDosesPer24h() == null
                    || cmd.maxDosesPer24h() < 1) {
                throw new ConditionNotMetException(List.of(new UnmetCondition(
                        "PRN_LIMITS_REQUIRED", "Đơn PRN bắt buộc có khoảng cách tối thiểu > 0 và số lần tối đa >= 1")));
            }
        }

        // 6. Kiểm tra trùng hoạt chất và dị ứng qua PrescriptionValidator (FR-007, FR-008, BR-M07-06)
        List<Prescription> existing = prescriptionRepository.findUnendedByResidentId(residentId);
        List<AllergyItemView> allergies = healthAllergyPort.getActiveAllergies(residentId);
        PrescriptionCheckResult checkResult =
                prescriptionValidator.validate(med.getAllergenIds(), existing, allergies, null);

        if (checkResult.hasUnconfirmedIssues(cmd.confirmedIssues())) {
            List<PrescriptionIssueResponse> unconfirmed =
                    checkResult.findUnconfirmedIssues(cmd.confirmedIssues()).stream()
                            .map(PrescriptionIssueResponse::from)
                            .toList();
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "ISSUES_REQUIRE_CONFIRMATION",
                    "Còn vấn đề trùng hoạt chất hoặc xung đột dị ứng chưa có lý do xác nhận (FR-007, BR-M07-06)",
                    Map.of("issues", unconfirmed));
        }

        // 7. Xác định trạng thái ban đầu (FR-011, research D-05)
        PrescriptionStatus initialStatus;
        if (cmd.pendingAdmission()) {
            initialStatus = PrescriptionStatus.PENDING_EFFECTIVE;
        } else if (cmd.startedAt().isAfter(businessTime.now())) {
            initialStatus = PrescriptionStatus.PENDING_EFFECTIVE;
        } else {
            initialStatus = PrescriptionStatus.ACTIVE;
        }

        Prescription p = new Prescription(
                residentId,
                cmd.medicationId(),
                cmd.dosage(),
                cmd.dosageUnit(),
                cmd.route(),
                cmd.prescriptionType(),
                cmd.frequencyType(),
                cmd.timedSlots(),
                cmd.everyNHours(),
                cmd.firstSlot(),
                cmd.daysOfWeek(),
                cmd.minIntervalMinutes(),
                cmd.maxDosesPer24h(),
                cmd.startedAt(),
                cmd.endedAt(),
                cmd.medicationSource(),
                cmd.prescriberType(),
                cmd.facilityName(),
                cmd.prescriberName(),
                cmd.prescriberId(),
                cmd.prescribedAt(),
                cmd.prescriptionFileId(),
                cmd.instructionNote(),
                initialStatus,
                cmd.pendingAdmission(),
                null,
                null,
                cmd.prescriberId(),
                businessTime.now(),
                med.getAllergenIds(),
                businessTime.now());

        Prescription saved = prescriptionRepository.save(p);

        // Lưu các vấn đề đã xác nhận vào prescription_issue
        if (!checkResult.issues().isEmpty()) {
            List<PrescriptionIssue> issuesToSave = new ArrayList<>();
            for (PrescriptionIssueView issueView : checkResult.issues()) {
                String reason =
                        cmd.confirmedIssues() != null ? cmd.confirmedIssues().get(issueView.issueCode()) : null;
                PrescriptionIssue issue = new PrescriptionIssue(
                        saved.getId(),
                        null,
                        null,
                        issueView.issueType(),
                        issueView.issueCode(),
                        issueView.duplicatePrescriptionId(),
                        issueView.allergyItemId(),
                        issueView.severity(),
                        issueView.activeIngredient(),
                        issueView.manualReviewRequired(),
                        reason,
                        cmd.prescriberId(),
                        businessTime.now(),
                        businessTime.now());
                issuesToSave.add(issue);
            }
            prescriptionIssueRepository.saveAll(issuesToSave);
        }

        return toResponse(saved);
    }

    @RequiresPermission(function = Function.PRESCRIPTION, operation = Operation.UPDATE)
    @Transactional
    public PrescriptionResponse updateInstructionNote(Target target, long id, String ifMatch, String newNote) {
        Prescription p = prescriptionRepository.findById(id).orElseThrow(NotFoundException::new);
        VersionEtag.requireMatch(ifMatch, p.getVersion(), toResponse(p));

        p.changeInstructionNote(newNote);
        Prescription saved = prescriptionRepository.save(p);
        return toResponse(saved);
    }

    private static ConditionNotMetException conditionNotMet(String code, String description) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, description)));
    }

    @RequiresPermission(function = Function.PRESCRIPTION, operation = Operation.UPDATE)
    @Transactional
    public PrescriptionResponse suspend(
            Target target, long id, OrderPrescriptionBody body, Long currentUserId, boolean isDoctor) {
        Prescription p = prescriptionRepository.findById(id).orElseThrow(NotFoundException::new);
        residentMedicationLock.lock(p.getResidentId());

        if (p.getStatus() != PrescriptionStatus.ACTIVE) {
            throw conditionNotMet("INVALID_STATE", "Prescription must be ACTIVE to suspend");
        }

        validateAndSaveBasis(p, body.getExternalBasis(), PrescriptionBasisCommand.SUSPEND, currentUserId, isDoctor);

        p.suspend(SuspensionReason.ORDER);
        Prescription saved = prescriptionRepository.save(p);

        // Chuyển các liều chưa đến giờ hoặc đến giờ thành TAM_DUNG
        if (doseRepository != null) {
            List<MedicationDose> openDoses = doseRepository.findByPrescriptionIdAndStatusIn(
                    p.getId(), List.of(DoseStatus.UPCOMING, DoseStatus.DUE));
            Instant now = businessTime.now();
            for (MedicationDose d : openDoses) {
                if (!d.getScheduledTime().isBefore(now.minusSeconds(60))) {
                    d.suspend(now);
                    doseRepository.save(d);
                }
            }
        }

        return toResponse(saved);
    }

    @RequiresPermission(function = Function.PRESCRIPTION, operation = Operation.UPDATE)
    @Transactional
    public PrescriptionResponse resume(
            Target target, long id, OrderPrescriptionBody body, Long currentUserId, boolean isDoctor) {
        Prescription p = prescriptionRepository.findById(id).orElseThrow(NotFoundException::new);
        residentMedicationLock.lock(p.getResidentId());

        if (p.getStatus() != PrescriptionStatus.SUSPENDED) {
            throw conditionNotMet("INVALID_STATE", "Prescription must be SUSPENDED to resume");
        }

        if (p.getSuspensionReason() == SuspensionReason.RECONCILIATION_PENDING) {
            throw conditionNotMet(
                    "CHO_DOI_CHIEU",
                    "Prescription suspended for reconciliation can only be resumed via reconciliation ticket");
        }

        if (reconciliationCheckPort.hasOpenOrPendingReconciliation(p.getResidentId())) {
            throw conditionNotMet("RECONCILIATION_IN_PROGRESS", "Cannot resume while reconciliation ticket is open");
        }

        validateAndSaveBasis(p, body.getExternalBasis(), PrescriptionBasisCommand.RESUME, currentUserId, isDoctor);

        p.resume();
        Prescription saved = prescriptionRepository.save(p);

        // Khôi phục các liều Tạm dừng nếu cuối cửa sổ chưa qua
        if (doseRepository != null) {
            List<MedicationDose> suspendedDoses =
                    doseRepository.findByPrescriptionIdAndStatusIn(p.getId(), List.of(DoseStatus.SUSPENDED));
            Instant now = businessTime.now();
            for (MedicationDose d : suspendedDoses) {
                if (d.getWindowTo().isAfter(now)) {
                    d.resumeFromSuspension(now);
                    doseRepository.save(d);
                }
            }
        }

        return toResponse(saved);
    }

    /**
     * Cảnh báo khi Ngừng đơn cũ sớm hơn hiệu lực của đơn thay thế (BR-M07-15, api-006 {@code warnings}): khoảng giữa
     * hai mốc không có liều. Không chặn lệnh.
     */
    @Transactional(readOnly = true)
    public List<String> discontinueWarnings(long id, Instant discontinueAt) {
        Instant at = discontinueAt != null ? discontinueAt : businessTime.now();
        return prescriptionRepository
                .findByReplacesId(id)
                .filter(next ->
                        next.getStartedAt() != null && next.getStartedAt().isAfter(at))
                .map(next -> List.of("NO_DOSE_GAP"))
                .orElse(List.of());
    }

    @RequiresPermission(function = Function.PRESCRIPTION, operation = Operation.UPDATE)
    @Transactional
    public PrescriptionResponse discontinue(
            Target target, long id, DiscontinuePrescriptionBody body, Long currentUserId, boolean isDoctor) {
        Prescription p = prescriptionRepository.findById(id).orElseThrow(NotFoundException::new);
        residentMedicationLock.lock(p.getResidentId());

        if (p.getStatus() == PrescriptionStatus.DISCONTINUED || p.getStatus() == PrescriptionStatus.EXPIRED) {
            throw conditionNotMet("FINAL_STATE", "Final status prescription cannot be discontinued");
        }

        Instant now = businessTime.now();
        Instant discontinueAt = body.getDiscontinueAt() != null ? body.getDiscontinueAt() : now;
        if (discontinueAt.isBefore(now.minusSeconds(60))) {
            throw conditionNotMet("INVALID_TIME", "Discontinuation time cannot be before command time");
        }

        validateAndSaveBasis(p, body.getExternalBasis(), PrescriptionBasisCommand.DISCONTINUE, currentUserId, isDoctor);

        // Trường hợp đặc biệt (BR-M07-15): Ngừng đơn thay thế còn PENDING_EFFECTIVE -> hủy thay đổi
        if (p.getStatus() == PrescriptionStatus.PENDING_EFFECTIVE && p.getReplacesId() != null) {
            p.discontinue(TerminationReason.DISCONTINUED);
            Prescription saved = prescriptionRepository.save(p);

            // Đơn cũ thôi bị ngừng tại thời điểm đã hẹn
            prescriptionRepository.findById(p.getReplacesId()).ifPresent(old -> {
                old.cancelScheduledSuspension();
                prescriptionRepository.save(old);
            });

            return toResponse(saved);
        }

        // Trường hợp thông thường: Ngừng ngay hoặc ngừng tương lai
        if (discontinueAt.isAfter(now)) {
            p.scheduleSuspensionFrom(discontinueAt);
            Prescription saved = prescriptionRepository.save(p);
            return toResponse(saved);
        } else {
            p.discontinue(TerminationReason.DISCONTINUED);
            Prescription saved = prescriptionRepository.save(p);

            // Hủy các liều chưa đóng từ thời điểm ngừng (BR-M07-07)
            if (doseRepository != null) {
                List<MedicationDose> openDoses = doseRepository.findByPrescriptionIdAndStatusIn(
                        p.getId(),
                        List.of(
                                DoseStatus.UPCOMING,
                                DoseStatus.DUE,
                                DoseStatus.LATE,
                                DoseStatus.SUSPENDED,
                                DoseStatus.LEAVE_MEDICATION));
                for (MedicationDose d : openDoses) {
                    if (!d.getScheduledTime().isBefore(discontinueAt)) {
                        d.cancel(DoseCancellationReason.DISCONTINUED, now);
                        doseRepository.save(d);
                    }
                }
            }
            return toResponse(saved);
        }
    }

    @RequiresPermission(function = Function.PRESCRIPTION, operation = Operation.UPDATE)
    @Transactional
    public PrescriptionResponse changeDosage(
            Target target, long id, ChangeDosageBody body, Long currentUserId, boolean isDoctor) {
        Prescription oldPrescription = prescriptionRepository.findById(id).orElseThrow(NotFoundException::new);
        residentMedicationLock.lock(oldPrescription.getResidentId());

        if (oldPrescription.getStatus() == PrescriptionStatus.DISCONTINUED
                || oldPrescription.getStatus() == PrescriptionStatus.EXPIRED) {
            throw conditionNotMet("FINAL_STATE", "Final status prescription cannot be changed");
        }

        Instant now = businessTime.now();
        Instant effectiveFrom = body.getEffectiveFrom() != null ? body.getEffectiveFrom() : now;
        if (effectiveFrom.isBefore(now.minusSeconds(60))) {
            throw conditionNotMet("INVALID_TIME", "Effective from time cannot be before command time");
        }

        validateAndSaveBasis(
                oldPrescription,
                body.getExternalBasis(),
                PrescriptionBasisCommand.CHANGE_DOSE,
                currentUserId,
                isDoctor);

        // Chuyển đổi và kiểm tra đơn thuốc mới (bỏ qua trùng hoạt chất với chính oldPrescription - BR-M07-06 scenario
        // 4)
        CreatePrescriptionCommand cmd = toCommand(body.getNewPrescription(), currentUserId, isDoctor);
        PrescriptionCheckResult checkResult =
                check(target, oldPrescription.getResidentId(), cmd, oldPrescription.getId());

        // Kiểm tra xem các vấn đề (issues) đã được người dùng xác nhận lý do chưa
        if (!checkResult.issues().isEmpty()) {
            boolean allConfirmed = checkResult.issues().stream()
                    .allMatch(issue -> cmd.confirmedIssues() != null
                            && cmd.confirmedIssues().containsKey(issue.issueCode())
                            && !cmd.confirmedIssues().get(issue.issueCode()).isBlank());
            if (!allConfirmed) {
                List<UnmetCondition> conditions = checkResult.issues().stream()
                        .map(i -> new UnmetCondition(
                                i.issueCode(),
                                i.activeIngredient() != null ? i.activeIngredient() : "Issue " + i.issueCode()))
                        .toList();
                throw new ConditionNotMetException(conditions);
            }
        }

        Medication med = medicationRepository.findById(cmd.medicationId()).orElseThrow(NotFoundException::new);

        // 1. Cập nhật đơn cũ: nếu hiệu lực ngay thì DA_NGUNG (lý do REPLACED); nếu tương lai thì đặt suspendedFrom
        if (effectiveFrom.isAfter(now)) {
            oldPrescription.scheduleSuspensionFrom(effectiveFrom);
            prescriptionRepository.save(oldPrescription);
        } else {
            oldPrescription.discontinue(TerminationReason.REPLACED);
            prescriptionRepository.save(oldPrescription);

            // Hủy các liều chưa đóng của đơn cũ từ thời điểm hiệu lực
            if (doseRepository != null) {
                List<MedicationDose> openDoses = doseRepository.findByPrescriptionIdAndStatusIn(
                        oldPrescription.getId(),
                        List.of(
                                DoseStatus.UPCOMING,
                                DoseStatus.DUE,
                                DoseStatus.LATE,
                                DoseStatus.SUSPENDED,
                                DoseStatus.LEAVE_MEDICATION));
                for (MedicationDose d : openDoses) {
                    if (!d.getScheduledTime().isBefore(effectiveFrom)) {
                        d.cancel(DoseCancellationReason.REPLACED, now);
                        doseRepository.save(d);
                    }
                }
            }
        }

        // 2. Tạo đơn thay thế mới
        PrescriptionStatus newStatus =
                effectiveFrom.isAfter(now) ? PrescriptionStatus.PENDING_EFFECTIVE : PrescriptionStatus.ACTIVE;

        Prescription newPrescription = new Prescription(
                oldPrescription.getResidentId(),
                cmd.medicationId(),
                cmd.dosage(),
                cmd.dosageUnit(),
                cmd.route(),
                cmd.prescriptionType(),
                cmd.frequencyType(),
                cmd.timedSlots(),
                cmd.everyNHours(),
                cmd.firstSlot(),
                cmd.daysOfWeek(),
                cmd.minIntervalMinutes(),
                cmd.maxDosesPer24h(),
                effectiveFrom,
                cmd.endedAt(),
                cmd.medicationSource(),
                cmd.prescriberType(),
                cmd.facilityName(),
                cmd.prescriberName(),
                cmd.prescriberId(),
                cmd.prescribedAt(),
                cmd.prescriptionFileId(),
                cmd.instructionNote(),
                newStatus,
                false,
                null,
                null,
                currentUserId,
                now,
                med.getAllergenIds(),
                businessTime.now());

        newPrescription.markReplaces(oldPrescription.getId());
        Prescription savedNew = prescriptionRepository.save(newPrescription);

        // Lưu các vấn đề đã xác nhận vào prescription_issue
        if (!checkResult.issues().isEmpty()) {
            List<PrescriptionIssue> issuesToSave = new ArrayList<>();
            for (PrescriptionIssueView issueView : checkResult.issues()) {
                String reason =
                        cmd.confirmedIssues() != null ? cmd.confirmedIssues().get(issueView.issueCode()) : null;
                PrescriptionIssue issue = new PrescriptionIssue(
                        savedNew.getId(),
                        null,
                        null,
                        issueView.issueType(),
                        issueView.issueCode(),
                        issueView.duplicatePrescriptionId(),
                        issueView.allergyItemId(),
                        issueView.severity(),
                        issueView.activeIngredient(),
                        issueView.manualReviewRequired(),
                        reason,
                        currentUserId,
                        now,
                        businessTime.now());
                issuesToSave.add(issue);
            }
            prescriptionIssueRepository.saveAll(issuesToSave);
        }

        return toResponse(savedNew);
    }

    @RequiresPermission(function = Function.PRESCRIPTION, operation = Operation.UPDATE)
    @Transactional
    public PrescriptionResponse extend(
            Target target, long id, ExtendPrescriptionBody body, Long currentUserId, boolean isDoctor) {
        Prescription p = prescriptionRepository.findById(id).orElseThrow(NotFoundException::new);
        residentMedicationLock.lock(p.getResidentId());

        if (p.getStatus() != PrescriptionStatus.ACTIVE && p.getStatus() != PrescriptionStatus.SUSPENDED) {
            throw conditionNotMet("INVALID_STATE", "Only ACTIVE or SUSPENDED prescriptions can be extended");
        }

        LocalDate newEndedAt = body.getNewEndedAt();
        if (newEndedAt != null && p.getEndedAt() != null && !newEndedAt.isAfter(p.getEndedAt())) {
            throw conditionNotMet("INVALID_END_DATE", "New end date must be strictly after current end date (FR-015)");
        }

        validateAndSaveBasis(p, body.getExternalBasis(), PrescriptionBasisCommand.EXTEND, currentUserId, isDoctor);

        p.changeEndDate(newEndedAt);
        if (newEndedAt == null
                || newEndedAt.isAfter(businessTime
                        .today()
                        .plusDays(parameters
                                .read(ParameterCode.CFG_M07_04)
                                .asDuration()
                                .toDays()))) {
            p.clearExpirationWarning();
        }

        Prescription saved = prescriptionRepository.save(p);
        return toResponse(saved);
    }

    private void validateAndSaveBasis(
            Prescription p,
            ExternalBasisBody basisBody,
            PrescriptionBasisCommand cmd,
            Long currentUserId,
            boolean isDoctor) {
        if (isDoctor) {
            if (basisBody != null
                    && basisBody.getFacilityName() != null
                    && !basisBody.getFacilityName().isBlank()) {
                saveBasis(p.getId(), basisBody, cmd, currentUserId);
            }
            return;
        }

        // Dành cho Điều dưỡng (hoặc phi bác sĩ)
        if (p.getPrescriberType() != PrescriberType.EXTERNAL) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "Only physicians can modify internal facility prescriptions (FR-011, FR-006)");
        }

        if (basisBody == null
                || basisBody.getFacilityName() == null
                || basisBody.getFacilityName().isBlank()
                || basisBody.getPrescriberName() == null
                || basisBody.getPrescriberName().isBlank()
                || basisBody.getPrescribedDate() == null
                || basisBody.getBasisType() == null
                || basisBody.getBasisType().isBlank()) {
            throw conditionNotMet(
                    "EXTERNAL_BASIS_REQUIRED",
                    "External prescription modification by nurse requires complete external basis (BR-M07-16, FR-013)");
        }

        PrescriptionBasisType type = parseBasisType(basisBody.getBasisType());
        if (type == null) {
            throw conditionNotMet("EXTERNAL_BASIS_REQUIRED", "Invalid basis type: " + basisBody.getBasisType());
        }

        if (type == PrescriptionBasisType.PHONE_ORDER) {
            if (basisBody.getCallerName() == null
                    || basisBody.getCallerName().isBlank()
                    || basisBody.getReceiverName() == null
                    || basisBody.getReceiverName().isBlank()
                    || basisBody.getCalledAt() == null) {
                throw conditionNotMet(
                        "EXTERNAL_BASIS_REQUIRED",
                        "Phone order basis requires callerName, receiverName, and calledAt (BR-M07-16)");
            }
        } else {
            if (basisBody.getDocumentFileId() == null) {
                throw conditionNotMet(
                        "EXTERNAL_BASIS_REQUIRED", "Document-based basis requires documentFileId (BR-M07-16)");
            }
        }

        saveBasis(p.getId(), basisBody, cmd, currentUserId);
    }

    private void saveBasis(
            Long prescriptionId, ExternalBasisBody basisBody, PrescriptionBasisCommand cmd, Long currentUserId) {
        PrescriptionBasisType type = parseBasisType(basisBody.getBasisType());
        PrescriptionBasis basis = new PrescriptionBasis(
                prescriptionId,
                cmd,
                basisBody.getFacilityName(),
                basisBody.getPrescriberName(),
                basisBody.getPrescribedDate(),
                type != null ? type : PrescriptionBasisType.NEW_PRESCRIPTION,
                basisBody.getCallerName(),
                basisBody.getReceiverName(),
                basisBody.getCalledAt(),
                basisBody.getDocumentFileId(),
                currentUserId,
                businessTime.now(),
                businessTime.now());
        prescriptionBasisRepository.save(basis);
    }

    private PrescriptionBasisType parseBasisType(String s) {
        if (s == null) return null;
        return switch (s.toUpperCase()) {
            case "DON_MOI", "NEW_PRESCRIPTION" -> PrescriptionBasisType.NEW_PRESCRIPTION;
            case "GIAY_RA_VIEN", "DISCHARGE_PAPER" -> PrescriptionBasisType.DISCHARGE_PAPER;
            case "DIEN_THOAI", "PHONE_ORDER" -> PrescriptionBasisType.PHONE_ORDER;
            default -> {
                try {
                    yield PrescriptionBasisType.valueOf(s.toUpperCase());
                } catch (Exception e) {
                    yield null;
                }
            }
        };
    }

    // --- Triển khai PrescriptionApi (M07.api) ---

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionView> unendedFacilityPrescriptions(long residentId) {
        return prescriptionRepository.findUnendedFacilityByResidentId(residentId).stream()
                .map(this::toPrescriptionView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveMedicationView> activeMedications(long residentId) {
        return prescriptionRepository.findUnendedByResidentId(residentId).stream()
                .map(p -> {
                    Medication med =
                            medicationRepository.findById(p.getMedicationId()).orElse(null);
                    String medName = med != null ? med.getName() : "Unknown";
                    String strength = med != null ? med.getConcentration() : "";
                    String form = med != null ? med.getDosageForm() : "";
                    MedicationSummary summary = new MedicationSummary(p.getMedicationId(), medName, "", strength, form);
                    List<java.time.LocalTime> scheduledTimes = java.util.Collections.emptyList();
                    if (p.getTimedSlots() != null && !p.getTimedSlots().isBlank()) {
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper =
                                    new com.fasterxml.jackson.databind.ObjectMapper();
                            String[] arr = mapper.readValue(p.getTimedSlots(), String[].class);
                            scheduledTimes = java.util.Arrays.stream(arr)
                                    .map(java.time.LocalTime::parse)
                                    .toList();
                        } catch (Exception ignored) {
                        }
                    }
                    String prnLimits = "";
                    if (p.getMinIntervalMinutes() != null || p.getMaxDosesPer24h() != null) {
                        prnLimits = String.format(
                                "minInterval: %s min, max24h: %s", p.getMinIntervalMinutes(), p.getMaxDosesPer24h());
                    }
                    return new ActiveMedicationView(
                            p.getId(),
                            summary,
                            p.getDosage() != null ? p.getDosage().toPlainString() + " " + p.getDosageUnit() : "",
                            p.getRoute(),
                            scheduledTimes,
                            prnLimits,
                            p.getMedicationSource(),
                            p.getStatus().name());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionView> history(long residentId) {
        return prescriptionRepository.findHistoryByResidentId(residentId).stream()
                .map(this::toPrescriptionView)
                .toList();
    }

    // --- Helper mappers ---

    private PrescriptionView toPrescriptionView(Prescription p) {
        Medication med = medicationRepository.findById(p.getMedicationId()).orElse(null);
        MedicationSummary summary = med != null
                ? new MedicationSummary(med.getId(), med.getName(), "", med.getConcentration(), med.getDosageForm())
                : new MedicationSummary(p.getMedicationId(), "", "", "", "");

        Long replacedBy = prescriptionRepository
                .findByReplacesId(p.getId())
                .map(Prescription::getId)
                .orElse(null);

        return new PrescriptionView(
                p.getId(),
                p.getResidentId(),
                p.getPrescriptionType().name(),
                p.getStatus().name(),
                p.getMedicationSource(),
                p.getStartedAt() != null
                        ? LocalDate.ofInstant(p.getStartedAt(), java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                        : null,
                p.getEndedAt(),
                summary,
                p.getInstructionNote(),
                replacedBy);
    }

    private PrescriptionResponse toResponse(Prescription p) {
        Long replacedBy = prescriptionRepository
                .findByReplacesId(p.getId())
                .map(Prescription::getId)
                .orElse(null);
        List<PrescriptionIssue> issues = prescriptionIssueRepository.findByPrescriptionId(p.getId());

        List<String> allowedCommands = new ArrayList<>();
        if (p.getStatus() == PrescriptionStatus.ACTIVE) {
            allowedCommands.add("SUSPEND");
            allowedCommands.add("DISCONTINUE");
            allowedCommands.add("CHANGE_DOSE");
            allowedCommands.add("EXTEND");
        } else if (p.getStatus() == PrescriptionStatus.SUSPENDED) {
            allowedCommands.add("RESUME");
            allowedCommands.add("DISCONTINUE");
            allowedCommands.add("EXTEND");
        }

        return PrescriptionResponse.from(p, replacedBy, false, issues, List.of(), allowedCommands, List.of());
    }
}
