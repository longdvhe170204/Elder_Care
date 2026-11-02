package com.eldercare.backend.medication.internal.governance.service;

import static java.util.stream.Collectors.toMap;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CurrentValue;
import com.eldercare.backend.medication.api.MedicationStatisticsApi;
import com.eldercare.backend.medication.api.dto.DoseReportView;
import com.eldercare.backend.medication.api.dto.DoseStatus;
import com.eldercare.backend.medication.api.dto.ReconciliationReportView;
import com.eldercare.backend.medication.api.spi.NursingShiftPort;
import com.eldercare.backend.medication.internal.dose.domain.DoseAuthorityBasis;
import com.eldercare.backend.medication.internal.dose.domain.DoseResult;
import com.eldercare.backend.medication.internal.dose.domain.DoseResultType;
import com.eldercare.backend.medication.internal.dose.domain.DoseReview;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.persistence.DoseReviewRepository;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.dose.service.DoseResultOfflineHandler;
import com.eldercare.backend.medication.internal.reconciliation.domain.MedicationReconciliation;
import com.eldercare.backend.medication.internal.reconciliation.persistence.MedicationReconciliationRepository;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultMedicationStatisticsService implements MedicationStatisticsApi {

    private final MedicationDoseRepository doseRepo;
    private final MedicationReconciliationRepository reconciliationRepo;
    private final DoseResultRepository resultRepo;
    private final DoseReviewRepository reviewRepo;
    private final NursingShiftPort shifts;
    private final CorrectionApi corrections;

    public DefaultMedicationStatisticsService(
            MedicationDoseRepository doseRepo,
            MedicationReconciliationRepository reconciliationRepo,
            DoseResultRepository resultRepo,
            DoseReviewRepository reviewRepo,
            NursingShiftPort shifts,
            CorrectionApi corrections) {
        this.doseRepo = doseRepo;
        this.reconciliationRepo = reconciliationRepo;
        this.resultRepo = resultRepo;
        this.reviewRepo = reviewRepo;
        this.shifts = shifts;
        this.corrections = corrections;
    }

    @Override
    public Stream<DoseReportView> dosesInPeriod(ScopeFilter scopeFilter, Instant from, Instant to) {
        List<MedicationDose> doses = doseRepo.findAll().stream()
                .filter(dose -> inPeriod(dose.getScheduledTime(), from, to))
                .filter(dose -> allowed(scopeFilter, dose.getFloorAreaId(), dose.getResidentId()))
                .toList();
        Map<Long, DoseResult> results =
                byId(resultRepo.findByDoseIdIn(ids(doses, MedicationDose::getId)), DoseResult::getDoseId);
        List<Long> resultIds = ids(results.values(), DoseResult::getId);
        Map<Long, DoseReview> reviews = byId(reviewRepo.findByDoseResultIdIn(resultIds), DoseReview::getDoseResultId);
        Map<Long, CurrentValue> current =
                resultIds.isEmpty() ? Map.of() : corrections.current(DoseResultOfflineHandler.RECORD_TYPE, resultIds);

        return doses.stream().map(dose -> report(dose, results.get(dose.getId()), reviews, current));
    }

    @Override
    public Stream<ReconciliationReportView> reconciliationsInPeriod(ScopeFilter scopeFilter, Instant from, Instant to) {
        List<MedicationReconciliation> list = reconciliationRepo.findAll();
        return list.stream()
                .filter(r -> inPeriod(r.getCreatedAt(), from, to))
                .filter(r -> allowed(scopeFilter, null, r.getResidentId()))
                .map(r -> new ReconciliationReportView(
                        r.getId(),
                        r.getResidentId(),
                        r.getReason().name(),
                        r.getStatus().name(),
                        r.getDeadlineAt(),
                        r.getOverdueAt() != null));
    }

    private DoseReportView report(
            MedicationDose dose, DoseResult result, Map<Long, DoseReview> reviews, Map<Long, CurrentValue> current) {
        long floorId = dose.getFloorAreaId() != null ? dose.getFloorAreaId() : 0L;
        Long shiftId = dose.getFloorAreaId() == null
                ? null
                : shifts.shiftAt(dose.getFloorAreaId(), dose.getScheduledTime())
                        .map(shift -> shift.id())
                        .orElse(null);
        DoseReview review = result == null ? null : reviews.get(result.getId());
        boolean floorShared = result != null && result.getAuthorityBasis() == DoseAuthorityBasis.FLOOR_SHARED;
        boolean dispensed =
                dose.getStatus() == com.eldercare.backend.medication.internal.dose.domain.DoseStatus.LEAVE_MEDICATION
                        || result != null && result.getAuthorityBasis() == DoseAuthorityBasis.LEAVE_MEDICATION;

        return new DoseReportView(
                MedicationScheduleCalculator.doseKey(dose.getPrescriptionId(), dose.getScheduledTime()),
                dose.getResidentId(),
                floorId,
                shiftId,
                dose.getScheduledTime(),
                currentStatus(dose, result, result == null ? null : current.get(result.getId())),
                result != null ? result.getRecorderId() : dose.getClosedById(),
                dose.getClosureRule(),
                floorShared,
                dispensed,
                result != null && result.isLateFlag(),
                result != null && result.isOfflineFlag(),
                review == null ? null : review.getStatus().name());
    }

    private static DoseStatus currentStatus(MedicationDose dose, DoseResult result, CurrentValue current) {
        DoseResultType corrected = correctedResultType(current);
        if (corrected != null) {
            return map(corrected);
        }
        if (result != null) {
            return map(result.getResultType());
        }
        return switch (dose.getStatus()) {
            case UPCOMING -> DoseStatus.SCHEDULED;
            case DUE -> DoseStatus.DUE;
            case LATE -> DoseStatus.LATE;
            case MISSED -> DoseStatus.MISSED;
            case ADMINISTERED -> DoseStatus.ADMINISTERED;
            case REFUSED -> DoseStatus.REFUSED;
            case NOT_ADMINISTERED -> DoseStatus.NOT_ADMINISTERED;
            case SUSPENDED -> DoseStatus.SUSPENDED;
            case LEAVE_MEDICATION -> DoseStatus.DISPENSED_FOR_TRIP;
            case CANCELLED -> DoseStatus.CANCELLED;
        };
    }

    private static DoseStatus map(DoseResultType resultType) {
        return switch (resultType) {
            case ADMINISTERED -> DoseStatus.ADMINISTERED;
            case REFUSED -> DoseStatus.REFUSED;
            case NOT_ADMINISTERED -> DoseStatus.NOT_ADMINISTERED;
            case MISSED -> DoseStatus.MISSED;
        };
    }

    private static DoseResultType correctedResultType(CurrentValue current) {
        if (current == null || !current.corrected() || current.voided()) {
            return null;
        }
        if (current.content() instanceof Map<?, ?> content && content.get("resultType") instanceof String value) {
            try {
                return DoseResultType.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean inPeriod(Instant value, Instant from, Instant to) {
        return value != null && !value.isBefore(from) && value.isBefore(to);
    }

    private static boolean allowed(ScopeFilter scope, Long floorId, Long residentId) {
        return scope.facilityWide()
                || floorId != null && scope.floorAreaIds().contains(floorId)
                || residentId != null && scope.residentIds().contains(residentId);
    }

    private static <T> List<Long> ids(Collection<T> rows, Function<T, Long> id) {
        return rows.stream().map(id).filter(java.util.Objects::nonNull).toList();
    }

    private static <T> Map<Long, T> byId(Collection<T> rows, Function<T, Long> id) {
        return rows.stream().collect(toMap(id, Function.identity()));
    }
}
