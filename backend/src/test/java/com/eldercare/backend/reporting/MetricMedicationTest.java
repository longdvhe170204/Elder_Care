package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.medication.api.MedicationStatisticsApi;
import com.eldercare.backend.medication.api.dto.DoseReportView;
import com.eldercare.backend.medication.api.dto.DoseStatus;
import com.eldercare.backend.medication.api.dto.ReconciliationReportView;
import com.eldercare.backend.reporting.internal.medication.service.DoseRecords;
import com.eldercare.backend.reporting.internal.medication.service.MissedDoseRateMetric;
import com.eldercare.backend.reporting.internal.medication.service.OverdueReconciliationsMetric;
import com.eldercare.backend.reporting.internal.medication.service.RefusedDoseRateMetric;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.support.fake.medication.FakeMedicationStatistics;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Chỉ tiêu liều thuốc và phiếu đối chiếu (tên logic {@code THUOC.TY_LE_BO_LO}, {@code THUOC.TY_LE_TU_CHOI},
 * {@code THUOC.DOI_CHIEU_QUA_HAN}; 016 T107, FR-043, FR-044) theo định nghĩa 006 FR-051.
 */
class MetricMedicationTest {

    private static final Instant FROM = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-08T00:00:00Z");

    private final FakeMedicationStatistics statistics = new FakeMedicationStatistics();
    private final DoseRecords doses = new DoseRecords(new StaticListableBeanFactory(Map.of("statistics", statistics))
            .getBeanProvider(MedicationStatisticsApi.class));
    private final MissedDoseRateMetric missed = new MissedDoseRateMetric(doses);
    private final RefusedDoseRateMetric refused = new RefusedDoseRateMetric(doses);
    private final OverdueReconciliationsMetric reconciliations =
            new OverdueReconciliationsMetric(new StaticListableBeanFactory(Map.of("statistics", statistics))
                    .getBeanProvider(MedicationStatisticsApi.class));

    private static DoseReportView dose(
            String key, Long shift, DoseStatus status, Long closedBy, boolean offline, String review) {
        return new DoseReportView(
                key,
                7L,
                101L,
                shift,
                FROM.plusSeconds(3600),
                status,
                closedBy,
                null,
                false,
                false,
                false,
                offline,
                review);
    }

    private <R extends MetricRecord> MetricValue value(MetricSource<R> source) {
        Metric<R> metric = source.metric();
        return metric.compute(
                metric.included(source.records(ScopeFilter.FACILITY, FROM, TO, ReportFilters.NONE), r -> true));
    }

    @Test
    void FR_043_ratesOverFinalDoses_excludingCancelledSuspendedAndSystemNotAdministered() {
        statistics.doses(List.of(
                dose("d1", 1L, DoseStatus.ADMINISTERED, 5L, false, null),
                dose("d2", 1L, DoseStatus.REFUSED, 5L, false, null),
                dose("d3", 2L, DoseStatus.MISSED, null, false, null),
                dose("d4", 2L, DoseStatus.NOT_ADMINISTERED, 5L, true, "PENDING_REVIEW"),
                dose("d5", null, DoseStatus.NOT_ADMINISTERED, null, false, null),
                dose("d6", 1L, DoseStatus.CANCELLED, null, false, null),
                dose("d7", 1L, DoseStatus.SUSPENDED, null, false, null),
                dose("d8", 1L, DoseStatus.DISPENSED_FOR_TRIP, null, false, null),
                dose("d9", 1L, DoseStatus.SCHEDULED, null, false, null)));

        MetricValue missedValue = value(missed);
        MetricValue refusedValue = value(refused);

        assertThat(missed.metric().code()).isEqualTo("MED.MISSED_RATE");
        assertThat(refused.metric().code()).isEqualTo("MED.REFUSED_RATE");
        assertThat(missed.metric().group()).isEqualTo(MetricGroup.MEDICATION_DOSES);
        assertThat(missedValue.value()).isEqualByComparingTo("25.0");
        assertThat(missedValue.numerator()).isEqualByComparingTo("1");
        assertThat(missedValue.denominator()).isEqualByComparingTo("4");
        assertThat(refusedValue.value()).isEqualByComparingTo("25.0");
        assertThat(missedValue.pendingReviewCount()).isEqualTo(1);
        assertThat(missedValue.breakdown())
                .extracting(MetricValue.Breakdown::key, b -> String.valueOf(b.value()))
                .containsExactly(Tuple.tuple("1", "0.0"), Tuple.tuple("2", "50.0"), Tuple.tuple("NO_SHIFT", "null"));
    }

    @Test
    void FR_044_overdueReconciliations_openOrPendingConfirmation_withOverdueMark() {
        Instant deadline = FROM;
        statistics.reconciliations(List.of(
                new ReconciliationReportView(1, 7, "ADMISSION", "OPEN", deadline, true),
                new ReconciliationReportView(2, 8, "HOSPITAL_RETURN", "PENDING_CONFIRMATION", deadline, true),
                new ReconciliationReportView(3, 7, "ADMISSION", "CONFIRMED", deadline, true),
                new ReconciliationReportView(4, 9, "ADMISSION", "OPEN", deadline, false)));

        MetricValue v = value(reconciliations);

        assertThat(reconciliations.metric().code()).isEqualTo("MED.OVERDUE_RECONCILIATIONS");
        assertThat(reconciliations.metric().group()).isEqualTo(MetricGroup.MEDICATION_RECONCILIATIONS);
        assertThat(v.value()).isEqualByComparingTo("2");
        assertThat(reconciliations
                        .metric()
                        .details(
                                reconciliations
                                        .metric()
                                        .included(
                                                reconciliations.records(
                                                        ScopeFilter.FACILITY, FROM, TO, ReportFilters.NONE),
                                                r -> true),
                                null))
                .extracting(r -> r.residentId())
                .containsExactlyInAnyOrder(7L, 8L);
    }
}
