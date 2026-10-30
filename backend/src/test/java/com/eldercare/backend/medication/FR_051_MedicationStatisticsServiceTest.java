package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.medication.api.dto.DoseReportView;
import com.eldercare.backend.medication.api.dto.DoseStatus;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.dto.NursingShiftView;
import com.eldercare.backend.medication.api.dto.ReconciliationReportView;
import com.eldercare.backend.medication.api.spi.NursingShiftPort;
import com.eldercare.backend.medication.internal.domain.DoseAuthorityBasis;
import com.eldercare.backend.medication.internal.domain.DoseResult;
import com.eldercare.backend.medication.internal.domain.DoseResultType;
import com.eldercare.backend.medication.internal.domain.DoseReview;
import com.eldercare.backend.medication.internal.domain.MedicationDose;
import com.eldercare.backend.medication.internal.domain.MedicationReconciliation;
import com.eldercare.backend.medication.internal.domain.ReconciliationReason;
import com.eldercare.backend.medication.internal.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.persistence.DoseReviewRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationReconciliationRepository;
import com.eldercare.backend.medication.internal.service.DefaultMedicationStatisticsService;
import com.eldercare.backend.medication.internal.service.DoseResultOfflineHandler;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.CurrentValue;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FR_051_MedicationStatisticsServiceTest {

    private MedicationDoseRepository doses;
    private MedicationReconciliationRepository reconciliations;
    private DoseResultRepository results;
    private DoseReviewRepository reviews;
    private NursingShiftPort shifts;
    private CorrectionApi corrections;
    private DefaultMedicationStatisticsService statistics;

    @BeforeEach
    void setUp() {
        doses = mock(MedicationDoseRepository.class);
        reconciliations = mock(MedicationReconciliationRepository.class);
        results = mock(DoseResultRepository.class);
        reviews = mock(DoseReviewRepository.class);
        shifts = mock(NursingShiftPort.class);
        corrections = mock(CorrectionApi.class);
        statistics =
                new DefaultMedicationStatisticsService(doses, reconciliations, results, reviews, shifts, corrections);
    }

    @Test
    void FR_051_dosesUseCurrentResultScopeShiftAndReportingFlags() {
        Instant from = Instant.parse("2026-10-28T00:00:00Z");
        Instant scheduled = Instant.parse("2026-10-28T01:00:00Z");
        Instant to = Instant.parse("2026-10-28T03:00:00Z");
        MedicationDose included =
                dose(101L, 10L, 201L, 11L, scheduled, com.eldercare.backend.medication.internal.domain.DoseStatus.ADMINISTERED);
        MedicationDose outsideScope =
                dose(102L, 20L, 202L, 22L, scheduled, com.eldercare.backend.medication.internal.domain.DoseStatus.ADMINISTERED);
        MedicationDose atExclusiveEnd =
                dose(103L, 30L, 203L, 11L, to, com.eldercare.backend.medication.internal.domain.DoseStatus.ADMINISTERED);
        DoseResult result = result(501L, 101L, 201L, 11L, DoseAuthorityBasis.FLOOR_SHARED, true, true);
        DoseReview review = new DoseReview(501L, null, 11L, "[88]", null, scheduled);
        review.accept(88L, null, scheduled.plusSeconds(60));

        when(doses.findAll()).thenReturn(List.of(included, outsideScope, atExclusiveEnd));
        when(results.findByDoseIdIn(List.of(101L))).thenReturn(List.of(result));
        when(reviews.findByDoseResultIdIn(List.of(501L))).thenReturn(List.of(review));
        when(corrections.current(DoseResultOfflineHandler.RECORD_TYPE, List.of(501L)))
                .thenReturn(Map.of(501L, new CurrentValue(Map.of("resultType", "REFUSED"), true, false)));
        when(shifts.shiftAt(11L, scheduled)).thenReturn(Optional.of(new NursingShiftView(91L, 11L, from, to)));

        List<DoseReportView> report = statistics
                .dosesInPeriod(new ScopeFilter(false, Set.of(11L), Set.of(), Set.of()), from, to)
                .toList();

        assertThat(report).singleElement().satisfies(row -> {
            assertThat(row.doseKey()).isEqualTo("10-202610280800");
            assertThat(row.residentId()).isEqualTo(201L);
            assertThat(row.floorAreaId()).isEqualTo(11L);
            assertThat(row.shiftId()).isEqualTo(91L);
            assertThat(row.finalStatus()).isEqualTo(DoseStatus.REFUSED);
            assertThat(row.closedByStaffId()).isEqualTo(77L);
            assertThat(row.floorSharedDose()).isTrue();
            assertThat(row.dispensedForTrip()).isFalse();
            assertThat(row.administeredLate()).isTrue();
            assertThat(row.recordedOffline()).isTrue();
            assertThat(row.reviewResult()).isEqualTo("ACCEPTED");
        });
    }

    @Test
    void FR_051_carriedDoseIsReportedAsDispensedForTrip() {
        Instant from = Instant.parse("2026-10-28T00:00:00Z");
        Instant scheduled = Instant.parse("2026-10-28T02:00:00Z");
        Instant to = Instant.parse("2026-10-28T03:00:00Z");
        MedicationDose carried = dose(
                104L, 40L, 204L, 12L, scheduled, com.eldercare.backend.medication.internal.domain.DoseStatus.LEAVE_MEDICATION);

        when(doses.findAll()).thenReturn(List.of(carried));
        when(results.findByDoseIdIn(List.of(104L))).thenReturn(List.of());
        when(shifts.shiftAt(12L, scheduled)).thenReturn(Optional.empty());

        List<DoseReportView> report = statistics
                .dosesInPeriod(new ScopeFilter(false, Set.of(), Set.of(), Set.of(204L)), from, to)
                .toList();

        assertThat(report).singleElement().satisfies(row -> {
            assertThat(row.finalStatus()).isEqualTo(DoseStatus.DISPENSED_FOR_TRIP);
            assertThat(row.dispensedForTrip()).isTrue();
            assertThat(row.shiftId()).isNull();
        });
    }

    @Test
    void FR_051_reconciliationsUseResidentScopeAndHalfOpenPeriod() {
        Instant from = Instant.parse("2026-10-28T00:00:00Z");
        Instant to = Instant.parse("2026-10-29T00:00:00Z");
        MedicationReconciliation included = reconciliation(601L, 201L, from);
        included.markOverdue(from.plusSeconds(3600));
        MedicationReconciliation outsideScope = reconciliation(602L, 202L, from.plusSeconds(60));
        MedicationReconciliation atExclusiveEnd = reconciliation(603L, 201L, to);
        when(reconciliations.findAll()).thenReturn(List.of(included, outsideScope, atExclusiveEnd));

        List<ReconciliationReportView> report = statistics
                .reconciliationsInPeriod(new ScopeFilter(false, Set.of(), Set.of(), Set.of(201L)), from, to)
                .toList();

        assertThat(report).singleElement().satisfies(row -> {
            assertThat(row.reconciliationId()).isEqualTo(601L);
            assertThat(row.residentId()).isEqualTo(201L);
            assertThat(row.isOverdue()).isTrue();
        });
    }

    private static MedicationDose dose(
            long id,
            long prescriptionId,
            long residentId,
            long floorId,
            Instant scheduled,
            com.eldercare.backend.medication.internal.domain.DoseStatus status) {
        MedicationDose dose = new MedicationDose(
                prescriptionId,
                residentId,
                1L,
                scheduled,
                scheduled.minusSeconds(1800),
                scheduled.plusSeconds(1800),
                BigDecimal.ONE,
                "tablet",
                MedicationSource.FACILITY,
                false,
                status,
                floorId,
                scheduled.minusSeconds(3600));
        setId(dose, id);
        return dose;
    }

    private static DoseResult result(
            long id,
            long doseId,
            long residentId,
            long floorId,
            DoseAuthorityBasis authority,
            boolean late,
            boolean offline) {
        DoseResult result = new DoseResult(
                doseId,
                residentId,
                floorId,
                DoseResultType.ADMINISTERED,
                Instant.parse("2026-10-28T01:10:00Z"),
                null,
                77L,
                authority,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                late,
                offline,
                false,
                null,
                null,
                Instant.parse("2026-10-28T01:11:00Z"));
        setId(result, id);
        return result;
    }

    private static MedicationReconciliation reconciliation(long id, long residentId, Instant createdAt) {
        MedicationReconciliation reconciliation = new MedicationReconciliation(
                residentId,
                ReconciliationReason.ADMISSION,
                null,
                createdAt.plusSeconds(7200),
                1L,
                createdAt,
                createdAt);
        setId(reconciliation, id);
        return reconciliation;
    }

    private static void setId(Object entity, long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
