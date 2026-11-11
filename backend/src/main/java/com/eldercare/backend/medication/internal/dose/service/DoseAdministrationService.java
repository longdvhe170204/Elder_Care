package com.eldercare.backend.medication.internal.dose.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.AdministerDoseRequest;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.dto.RefuseDoseRequest;
import com.eldercare.backend.medication.api.dto.SkipDoseRequest;
import com.eldercare.backend.medication.api.events.BilledSource;
import com.eldercare.backend.medication.api.events.MedicationAlertRequestedEvent;
import com.eldercare.backend.medication.api.events.MedicationBilledEvent;
import com.eldercare.backend.medication.internal.catalog.domain.Medication;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.DoseAuthorityBasis;
import com.eldercare.backend.medication.internal.dose.domain.DoseDetailView;
import com.eldercare.backend.medication.internal.dose.domain.DoseResult;
import com.eldercare.backend.medication.internal.dose.domain.DoseResultType;
import com.eldercare.backend.medication.internal.dose.domain.DoseReview;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.domain.DoseTransitionTable;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.persistence.DoseReviewRepository;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.family.service.FamilyMedicationService;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.persistence.ResidentMedicationLock;
import com.eldercare.backend.medication.internal.schedule.service.AbsenceScheduleContexts;
import com.eldercare.backend.medication.internal.schedule.service.CalculatedDoseSlot;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service quản lý xác nhận liều thuốc (Đã dùng, Từ chối, Không thực hiện; research D-08, D-11; FR-020 -> FR-033).
 */
@Service
public class DoseAdministrationService {

    private final MedicationDoseRepository doseRepository;
    private final DoseResultRepository doseResultRepository;
    private final DoseReviewRepository doseReviewRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicationRepository medicationRepository;
    private final MedicationScheduleCalculator scheduleCalculator;
    private final DoseAssigneeResolver assigneeResolver;
    private final ResidentMedicationLock residentMedicationLock;
    private final ApplicationEventPublisher eventPublisher;
    private final BusinessTime businessTime;
    private final ParameterApi parameters;
    private final FamilyMedicationService familyMedications;

    private final AbsenceScheduleContexts absenceContexts;

    public DoseAdministrationService(
            MedicationDoseRepository doseRepository,
            DoseResultRepository doseResultRepository,
            DoseReviewRepository doseReviewRepository,
            PrescriptionRepository prescriptionRepository,
            MedicationRepository medicationRepository,
            MedicationScheduleCalculator scheduleCalculator,
            DoseAssigneeResolver assigneeResolver,
            ResidentMedicationLock residentMedicationLock,
            ApplicationEventPublisher eventPublisher,
            BusinessTime businessTime,
            ParameterApi parameters,
            FamilyMedicationService familyMedications,
            AbsenceScheduleContexts absenceContexts) {
        this.absenceContexts = absenceContexts;
        this.doseRepository = doseRepository;
        this.doseResultRepository = doseResultRepository;
        this.doseReviewRepository = doseReviewRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.medicationRepository = medicationRepository;
        this.scheduleCalculator = scheduleCalculator;
        this.assigneeResolver = assigneeResolver;
        this.residentMedicationLock = residentMedicationLock;
        this.eventPublisher = eventPublisher;
        this.businessTime = businessTime;
        this.parameters = parameters;
        this.familyMedications = familyMedications;
    }

    /**
     * Xác nhận Đã dùng liều thuốc (FR-020, FR-026, FR-033).
     */
    @Transactional
    public DoseDetailView administerDose(
            String doseKey, AdministerDoseRequest request, Long recorderId, Long floorAreaId, Instant now) {

        ParsedDoseKey parsed = parseDoseKey(doseKey);
        residentMedicationLock.lock(getResidentIdForPrescription(parsed.prescriptionId()));

        MedicationDose dose = getOrMaterializeDose(parsed, now, floorAreaId);
        rejectIfClosed(dose, administeredContent(request));

        // Check offline restrictions for special control drugs (BR-M07-20)
        if (dose.isSpecialControl() && request.offlineFlag()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "REQUIRES_ONLINE",
                    "Thuốc kiểm soát đặc biệt bắt buộc trực tuyến, không được đồng bộ ngoại tuyến (BR-M07-20)");
        }

        // FR-044 (Q-393): nguồn gia đình gửi cần còn lô Được sử dụng chưa hết hạn dùng; liều thuộc lần giao không áp
        if (dose.getMedicationSource() == MedicationSource.FAMILY_SENT
                && request.leaveHandoverId() == null
                && !familyMedications.hasUsableBatch(dose.getResidentId(), dose.getPrescriptionId())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "NO_MEDICATION_AVAILABLE",
                    "Đơn không còn lô thuốc gia đình gửi Được sử dụng chưa hết hạn dùng (FR-044)");
        }

        Instant administeredAt = request.administeredAt() != null ? request.administeredAt() : now;
        if (administeredAt.isAfter(now)) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("ADMINISTERED_AT_IN_FUTURE", "Thời điểm dùng không được sau hiện tại")));
        }

        // Determine authority basis
        DoseAuthorityBasis basis;
        if (request.reportedByType() != null && !request.reportedByType().isBlank()) {
            basis = DoseAuthorityBasis.REPORTED_BY_OTHERS;
        } else {
            DoseAssigneeResolver.DoseAssignee assignee =
                    assigneeResolver.resolve(dose.getResidentId(), floorAreaId, dose.getScheduledTime(), now);
            if (assignee.assignedNurseId() != null && assignee.assignedNurseId().equals(recorderId)) {
                basis = DoseAuthorityBasis.ASSIGNED;
            } else {
                basis = DoseAuthorityBasis.FLOOR_SHARED;
            }
        }

        boolean lateFlag = administeredAt.isAfter(dose.getWindowTo());

        // Close dose record
        dose.closeAdministered(recorderId, "EMPLOYEE", floorAreaId, now);
        doseRepository.save(dose);

        // Create append-only result
        DoseResult result = new DoseResult(
                dose.getId(),
                dose.getResidentId(),
                floorAreaId,
                DoseResultType.ADMINISTERED,
                administeredAt,
                request.deviceRecordedAt(),
                recorderId,
                basis,
                request.reportedByType(),
                request.reportedById(),
                request.reportedByName(),
                request.relationship(),
                request.adverseReaction(),
                null,
                request.note(),
                lateFlag,
                request.offlineFlag(),
                false,
                request.leaveHandoverId(),
                businessTime.now());
        doseResultRepository.save(result);

        // Facility source fee event (FR-033)
        if (dose.getMedicationSource() == MedicationSource.FACILITY && request.leaveHandoverId() == null) {
            eventPublisher.publishEvent(new MedicationBilledEvent(
                    BilledSource.DOSE,
                    result.getId(),
                    dose.getResidentId(),
                    dose.getMedicationId(),
                    dose.getDosage(),
                    dose.getDosageUnit(),
                    administeredAt));
        }

        // Adverse reaction alert (FR-031)
        if (request.adverseReaction() != null && !request.adverseReaction().isBlank()) {
            eventPublisher.publishEvent(new MedicationAlertRequestedEvent(
                    "PHAN_UNG_THUOC",
                    "TRUNG_BINH",
                    dose.getResidentId(),
                    "dose:" + dose.getId(),
                    "reaction:" + dose.getId(),
                    now,
                    List.of()));
        }

        // If special control recorded after reconnect, create review record (FR-029)
        if (dose.isSpecialControl()) {
            DoseReview review = new DoseReview(
                    result.getId(),
                    null,
                    floorAreaId,
                    null,
                    "Thuốc kiểm soát đặc biệt cần kiểm tra xác nhận",
                    businessTime.now());
            doseReviewRepository.save(review);
        }

        return toDetailView(dose, result, doseKey, now);
    }

    /**
     * Xác nhận Từ chối liều thuốc (FR-021, FR-032, BR-M07-13).
     */
    @Transactional
    public DoseDetailView refuseDose(
            String doseKey, RefuseDoseRequest request, Long recorderId, Long floorAreaId, Instant now) {

        if (request.reason() == null || request.reason().isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("REASON_REQUIRED", "Lý do từ chối dùng thuốc bắt buộc (FR-021)")));
        }

        ParsedDoseKey parsed = parseDoseKey(doseKey);
        residentMedicationLock.lock(getResidentIdForPrescription(parsed.prescriptionId()));

        MedicationDose dose = getOrMaterializeDose(parsed, now, floorAreaId);
        rejectIfClosed(dose, reasonContent(request.reason(), request.note(), request.offlineFlag()));

        dose.closeRefused(recorderId, "EMPLOYEE", floorAreaId, now);
        doseRepository.save(dose);

        DoseResult result = new DoseResult(
                dose.getId(),
                dose.getResidentId(),
                floorAreaId,
                DoseResultType.REFUSED,
                null,
                request.deviceRecordedAt(),
                recorderId,
                DoseAuthorityBasis.ASSIGNED,
                null,
                null,
                null,
                null,
                null,
                request.reason(),
                request.note(),
                false,
                request.offlineFlag(),
                false,
                null,
                businessTime.now());
        doseResultRepository.save(result);

        // Check consecutive refusals (BR-M07-13)
        checkConsecutiveRefusals(dose.getId(), dose.getResidentId(), dose.getMedicationId(), now);

        return toDetailView(dose, result, doseKey, now);
    }

    /**
     * Xác nhận Không thực hiện liều thuốc (FR-021).
     */
    @Transactional
    public DoseDetailView skipDose(
            String doseKey, SkipDoseRequest request, Long recorderId, Long floorAreaId, Instant now) {

        if (request.reason() == null || request.reason().isBlank()) {
            throw new ConditionNotMetException(List.of(
                    new UnmetCondition("REASON_REQUIRED", "Lý do không thực hiện liều thuốc bắt buộc (FR-021)")));
        }

        ParsedDoseKey parsed = parseDoseKey(doseKey);
        residentMedicationLock.lock(getResidentIdForPrescription(parsed.prescriptionId()));

        MedicationDose dose = getOrMaterializeDose(parsed, now, floorAreaId);
        rejectIfClosed(dose, reasonContent(request.reason(), request.note(), request.offlineFlag()));

        dose.closeNotAdministered(recorderId, "EMPLOYEE", floorAreaId, now);
        doseRepository.save(dose);

        DoseResult result = new DoseResult(
                dose.getId(),
                dose.getResidentId(),
                floorAreaId,
                DoseResultType.NOT_ADMINISTERED,
                null,
                request.deviceRecordedAt(),
                recorderId,
                DoseAuthorityBasis.ASSIGNED,
                null,
                null,
                null,
                null,
                null,
                request.reason(),
                request.note(),
                false,
                request.offlineFlag(),
                false,
                null,
                businessTime.now());
        doseResultRepository.save(result);

        return toDetailView(dose, result, doseKey, now);
    }

    /**
     * DBR-13 (quickstart Q8): lệnh thứ hai trên liều đã đóng trả 409 {@code DOSE_ALREADY_CLOSED} kèm người đóng, thời
     * điểm, trạng thái hiện hành và nội dung bị từ chối (api-006 {@code DoseAlreadyClosed}) để client chuyển thành đính
     * chính; liều do Hệ thống đóng (Bỏ lỡ) không có người đóng.
     */
    private static void rejectIfClosed(MedicationDose dose, Map<String, Object> rejectedContent) {
        if (!DoseTransitionTable.isTerminal(dose.getStatus())) {
            return;
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("currentStatus", dose.getStatus().name());
        if (dose.getClosedById() != null) {
            properties.put("closedBy", dose.getClosedById().toString());
        }
        if (dose.getClosedAt() != null) {
            properties.put("closedAt", dose.getClosedAt());
        }
        properties.put("rejectedContent", rejectedContent);
        throw new BusinessException(
                HttpStatus.CONFLICT,
                "DOSE_ALREADY_CLOSED",
                "Liều đã được xác nhận; nội dung được giữ để chuyển thành đính chính (DBR-13)",
                properties);
    }

    /** Nội dung lệnh Đã dùng theo tên trường của api-006 {@code DoseResult}. */
    private static Map<String, Object> administeredContent(AdministerDoseRequest request) {
        Map<String, Object> content = new LinkedHashMap<>();
        putIfPresent(content, "administeredAt", request.administeredAt());
        if (request.reportedByType() != null) {
            Map<String, Object> reportedBy = new LinkedHashMap<>();
            putIfPresent(reportedBy, "type", request.reportedByType());
            putIfPresent(reportedBy, "id", request.reportedById());
            putIfPresent(reportedBy, "fullName", request.reportedByName());
            putIfPresent(reportedBy, "relationship", request.relationship());
            content.put("reportedBy", reportedBy);
        }
        putIfPresent(content, "adverseReaction", request.adverseReaction());
        putIfPresent(content, "note", request.note());
        content.put("recordedAfterDisconnect", request.offlineFlag());
        return content;
    }

    /** Nội dung lệnh Từ chối, Không thực hiện theo tên trường của api-006 {@code DoseResult}. */
    private static Map<String, Object> reasonContent(String reason, String note, boolean offlineFlag) {
        Map<String, Object> content = new LinkedHashMap<>();
        putIfPresent(content, "reason", reason);
        putIfPresent(content, "note", note);
        content.put("recordedAfterDisconnect", offlineFlag);
        return content;
    }

    private static void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    /**
     * Lấy chi tiết liều thuốc theo doseKey.
     */
    @Transactional(readOnly = true)
    public DoseDetailView getDoseDetail(String doseKey, Instant now) {
        ParsedDoseKey parsed = parseDoseKey(doseKey);
        Optional<MedicationDose> optDose =
                doseRepository.findByPrescriptionIdAndScheduledTime(parsed.prescriptionId(), parsed.scheduledTime());

        if (optDose.isPresent()) {
            MedicationDose dose = optDose.get();
            Optional<DoseResult> optResult = doseResultRepository.findByDoseId(dose.getId());
            return toDetailView(dose, optResult.orElse(null), doseKey, now);
        }

        // Project dynamic slot if not yet materialized
        Prescription prescription =
                prescriptionRepository.findById(parsed.prescriptionId()).orElseThrow(NotFoundException::new);
        Medication medication =
                medicationRepository.findById(prescription.getMedicationId()).orElse(null);

        List<CalculatedDoseSlot> slots = scheduleCalculator.calculate(
                prescription,
                medication,
                parsed.scheduledTime().minusSeconds(10),
                parsed.scheduledTime().plusSeconds(10),
                now,
                absenceContexts.of(prescription.getResidentId()));

        if (slots.isEmpty()) {
            throw new NotFoundException();
        }

        CalculatedDoseSlot slot = slots.get(0);
        return new DoseDetailView(
                slot.doseKey(),
                null,
                slot.prescriptionId(),
                slot.residentId(),
                slot.medicationId(),
                slot.medicationName(),
                slot.dosage(),
                slot.dosageUnit(),
                slot.route(),
                slot.medicationSource(),
                slot.specialControl(),
                slot.scheduledTime(),
                slot.windowFrom(),
                slot.windowTo(),
                slot.status(),
                slot.cancellationReason(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                null,
                true);
    }

    /**
     * Kiểm tra số lần từ chối liên tiếp và phát cảnh báo nếu >= 3 (BR-M07-13).
     */
    private void checkConsecutiveRefusals(Long doseId, Long residentId, Long medicationId, Instant now) {
        List<MedicationDose> pastDoses =
                doseRepository.findByResidentIdAndMedicationIdOrderByScheduledTimeDesc(residentId, medicationId);

        List<ClosedDoseView> views = new ArrayList<>();
        for (MedicationDose d : pastDoses) {
            views.add(new ClosedDoseView(d.getId(), d.getScheduledTime(), d.getStatus()));
        }

        int count = ConsecutiveRefusalCounter.count(views);
        if (ConsecutiveRefusalCounter.meetsThreshold(
                count, parameters.read(ParameterCode.CFG_M05_04).asInt())) {
            eventPublisher.publishEvent(new MedicationAlertRequestedEvent(
                    "TU_CHOI_LIEN_TIEP",
                    "TRUNG_BINH",
                    residentId,
                    "dose:" + doseId,
                    "refusal:" + residentId + ":" + medicationId,
                    now,
                    List.of()));
        }
    }

    /**
     * Lấy hoặc vật hóa liều thuốc tại thời điểm dự kiến.
     */
    public MedicationDose getOrMaterializeDose(ParsedDoseKey parsed, Instant now, Long floorAreaId) {
        Optional<MedicationDose> existing =
                doseRepository.findByPrescriptionIdAndScheduledTime(parsed.prescriptionId(), parsed.scheduledTime());
        if (existing.isPresent()) {
            return existing.get();
        }

        Prescription prescription =
                prescriptionRepository.findById(parsed.prescriptionId()).orElseThrow(NotFoundException::new);
        Medication medication =
                medicationRepository.findById(prescription.getMedicationId()).orElse(null);

        int defaultWindow = scheduleCalculator.defaultWindowMinutes();
        int windowBefore = medication != null && medication.getBeforeWindowMinutes() != null
                ? medication.getBeforeWindowMinutes()
                : defaultWindow;
        int windowAfter = medication != null && medication.getAfterWindowMinutes() != null
                ? medication.getAfterWindowMinutes()
                : defaultWindow;

        Instant windowFrom = parsed.scheduledTime().minus(windowBefore, ChronoUnit.MINUTES);
        Instant windowTo = parsed.scheduledTime().plus(windowAfter, ChronoUnit.MINUTES);

        DoseStatus status;
        if (now.isBefore(windowFrom)) {
            status = DoseStatus.UPCOMING;
        } else if (!now.isAfter(windowTo)) {
            status = DoseStatus.DUE;
        } else {
            status = DoseStatus.LATE;
        }

        MedicationDose dose = new MedicationDose(
                prescription.getId(),
                prescription.getResidentId(),
                prescription.getMedicationId(),
                parsed.scheduledTime(),
                windowFrom,
                windowTo,
                prescription.getDosage(),
                prescription.getDosageUnit(),
                prescription.getMedicationSource(),
                medication != null && medication.isSpecialControl(),
                status,
                floorAreaId,
                businessTime.now());

        if (status == DoseStatus.DUE) {
            dose.markDue(now);
        } else if (status == DoseStatus.LATE) {
            dose.markDue(windowFrom);
            dose.markLate(now);
        }

        return doseRepository.save(dose);
    }

    private Long getResidentIdForPrescription(Long prescriptionId) {
        return prescriptionRepository
                .findById(prescriptionId)
                .map(Prescription::getResidentId)
                .orElse(0L);
    }

    private DoseDetailView toDetailView(MedicationDose dose, DoseResult result, String doseKey, Instant now) {
        Medication med = medicationRepository.findById(dose.getMedicationId()).orElse(null);
        String medName = med != null ? med.getName() : "Thuốc";

        DoseAssigneeResolver.DoseAssignee assignee =
                assigneeResolver.resolve(dose.getResidentId(), dose.getFloorAreaId(), dose.getScheduledTime(), now);

        return new DoseDetailView(
                doseKey,
                dose.getId(),
                dose.getPrescriptionId(),
                dose.getResidentId(),
                dose.getMedicationId(),
                medName,
                dose.getDosage(),
                dose.getDosageUnit(),
                med != null ? med.getDefaultRoute() : "Oral",
                dose.getMedicationSource(),
                dose.isSpecialControl(),
                dose.getScheduledTime(),
                dose.getWindowFrom(),
                dose.getWindowTo(),
                dose.getStatus(),
                dose.getCancellationReason(),
                result != null ? result.getAdministeredAt() : null,
                result != null ? result.getResultType() : null,
                result != null ? result.getAuthorityBasis() : null,
                result != null ? result.getRecorderId() : null,
                result != null ? result.getReason() : null,
                result != null ? result.getAdverseReaction() : null,
                result != null ? result.getNote() : null,
                result != null && result.isLateFlag(),
                result != null && result.isOfflineFlag(),
                assignee.assignedNurseId(),
                assignee.floorShared());
    }

    public record ParsedDoseKey(Long prescriptionId, Instant scheduledTime) {}

    public ParsedDoseKey parseDoseKey(String doseKey) {
        if (doseKey == null || !doseKey.contains("-")) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("INVALID_DOSE_KEY", "Mã liều thuốc không đúng định dạng (FR-018)")));
        }
        String[] parts = doseKey.split("-");
        try {
            Long prescriptionId = Long.parseLong(parts[0]);
            LocalDateTime ldt = LocalDateTime.parse(parts[1], DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            Instant scheduledTime =
                    ldt.atZone(MedicationScheduleCalculator.ZONE_VN).toInstant();
            return new ParsedDoseKey(prescriptionId, scheduledTime);
        } catch (Exception e) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("INVALID_DOSE_KEY", "Không phân giải được mã liều: " + doseKey)));
        }
    }
}
