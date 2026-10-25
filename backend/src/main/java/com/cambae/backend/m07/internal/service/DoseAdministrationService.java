package com.cambae.backend.m07.internal.service;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.m07.api.dto.AdministerDoseRequest;
import com.cambae.backend.m07.api.dto.DoseDetailView;
import com.cambae.backend.m07.api.dto.MedicationSource;
import com.cambae.backend.m07.api.dto.RefuseDoseRequest;
import com.cambae.backend.m07.api.dto.SkipDoseRequest;
import com.cambae.backend.m07.api.events.BilledSource;
import com.cambae.backend.m07.api.events.MedicationAlertRequestedEvent;
import com.cambae.backend.m07.api.events.MedicationBilledEvent;
import com.cambae.backend.m07.internal.domain.DoseAuthorityBasis;
import com.cambae.backend.m07.internal.domain.DoseResult;
import com.cambae.backend.m07.internal.domain.DoseResultType;
import com.cambae.backend.m07.internal.domain.DoseReview;
import com.cambae.backend.m07.internal.domain.DoseStatus;
import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.domain.MedicationDose;
import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.persistence.DoseResultRepository;
import com.cambae.backend.m07.internal.persistence.DoseReviewRepository;
import com.cambae.backend.m07.internal.persistence.MedicationDoseRepository;
import com.cambae.backend.m07.internal.persistence.MedicationRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionRepository;
import com.cambae.backend.m07.internal.persistence.ResidentMedicationLock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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

    public DoseAdministrationService(
            MedicationDoseRepository doseRepository,
            DoseResultRepository doseResultRepository,
            DoseReviewRepository doseReviewRepository,
            PrescriptionRepository prescriptionRepository,
            MedicationRepository medicationRepository,
            MedicationScheduleCalculator scheduleCalculator,
            DoseAssigneeResolver assigneeResolver,
            ResidentMedicationLock residentMedicationLock,
            ApplicationEventPublisher eventPublisher) {
        this.doseRepository = doseRepository;
        this.doseResultRepository = doseResultRepository;
        this.doseReviewRepository = doseReviewRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.medicationRepository = medicationRepository;
        this.scheduleCalculator = scheduleCalculator;
        this.assigneeResolver = assigneeResolver;
        this.residentMedicationLock = residentMedicationLock;
        this.eventPublisher = eventPublisher;
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

        // Check offline restrictions for special control drugs (BR-M07-20)
        if (dose.isSpecialControl() && request.offlineFlag()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "BAT_BUOC_TRUC_TUYEN",
                    "Thuốc kiểm soát đặc biệt bắt buộc trực tuyến, không được đồng bộ ngoại tuyến (BR-M07-20)");
        }

        Instant administeredAt = request.administeredAt() != null ? request.administeredAt() : now;
        if (administeredAt.isAfter(now)) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("THOI_DIEM_DUNG_TUONG_LAI", "Thời điểm dùng không được sau hiện tại")));
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
                request.familyMedicationLotId(),
                request.leaveHandoverId());
        doseResultRepository.save(result);

        // Facility source fee event (FR-033)
        if (dose.getMedicationSource() == MedicationSource.FACILITY && request.leaveHandoverId() == null) {
            eventPublisher.publishEvent(new MedicationBilledEvent(
                    BilledSource.DOSE,
                    result.getId(),
                    dose.getResidentId(),
                    dose.getMedicationId(),
                    dose.getDosage(),
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
                    result.getId(), null, floorAreaId, null, "Thuốc kiểm soát đặc biệt cần kiểm tra xác nhận");
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
                    List.of(new UnmetCondition("THIEU_LY_DO", "Lý do từ chối dùng thuốc bắt buộc (FR-021)")));
        }

        ParsedDoseKey parsed = parseDoseKey(doseKey);
        residentMedicationLock.lock(getResidentIdForPrescription(parsed.prescriptionId()));

        MedicationDose dose = getOrMaterializeDose(parsed, now, floorAreaId);

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
                null);
        doseResultRepository.save(result);

        // Check consecutive refusals (BR-M07-13)
        checkConsecutiveRefusals(dose.getResidentId(), dose.getMedicationId(), now);

        return toDetailView(dose, result, doseKey, now);
    }

    /**
     * Xác nhận Không thực hiện liều thuốc (FR-021).
     */
    @Transactional
    public DoseDetailView skipDose(
            String doseKey, SkipDoseRequest request, Long recorderId, Long floorAreaId, Instant now) {

        if (request.reason() == null || request.reason().isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("THIEU_LY_DO", "Lý do không thực hiện liều thuốc bắt buộc (FR-021)")));
        }

        ParsedDoseKey parsed = parseDoseKey(doseKey);
        residentMedicationLock.lock(getResidentIdForPrescription(parsed.prescriptionId()));

        MedicationDose dose = getOrMaterializeDose(parsed, now, floorAreaId);

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
                null);
        doseResultRepository.save(result);

        return toDetailView(dose, result, doseKey, now);
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
                ScheduleContext.empty(prescription.getResidentId()));

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
    private void checkConsecutiveRefusals(Long residentId, Long medicationId, Instant now) {
        List<MedicationDose> pastDoses =
                doseRepository.findByResidentIdAndMedicationIdOrderByScheduledTimeDesc(residentId, medicationId);

        List<ClosedDoseView> views = new ArrayList<>();
        for (MedicationDose d : pastDoses) {
            views.add(new ClosedDoseView(d.getId(), d.getScheduledTime(), d.getStatus()));
        }

        int count = ConsecutiveRefusalCounter.count(views);
        if (ConsecutiveRefusalCounter.meetsThreshold(count, ConsecutiveRefusalCounter.DEFAULT_REFUSAL_THRESHOLD)) {
            eventPublisher.publishEvent(new MedicationAlertRequestedEvent(
                    "TU_CHOI_LIEN_TIEP",
                    "TRUNG_BINH",
                    residentId,
                    "resident:" + residentId + ":med:" + medicationId,
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

        int windowBefore = medication != null && medication.getBeforeWindowMinutes() != null
                ? medication.getBeforeWindowMinutes()
                : MedicationScheduleCalculator.DEFAULT_WINDOW_MINUTES;
        int windowAfter = medication != null && medication.getAfterWindowMinutes() != null
                ? medication.getAfterWindowMinutes()
                : MedicationScheduleCalculator.DEFAULT_WINDOW_MINUTES;

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
                floorAreaId);

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
                    List.of(new UnmetCondition("MA_LIEU_KHONG_HOP_LE", "Mã liều thuốc không đúng định dạng (FR-018)")));
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
                    List.of(new UnmetCondition("MA_LIEU_KHONG_HOP_LE", "Không phân giải được mã liều: " + doseKey)));
        }
    }
}
