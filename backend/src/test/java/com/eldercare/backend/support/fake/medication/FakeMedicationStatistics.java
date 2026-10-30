package com.eldercare.backend.support.fake.medication;

import com.eldercare.backend.medication.api.MedicationStatisticsApi;
import com.eldercare.backend.medication.api.dto.DoseReportView;
import com.eldercare.backend.medication.api.dto.ReconciliationReportView;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

/** Bản giả cho test của 016; trả các dòng do test đặt, không thay truy vấn và lọc quyền thật của 006. */
public class FakeMedicationStatistics implements MedicationStatisticsApi {

    private List<DoseReportView> doses = List.of();
    private List<ReconciliationReportView> reconciliations = List.of();

    public FakeMedicationStatistics doses(List<DoseReportView> values) {
        doses = List.copyOf(values);
        return this;
    }

    public FakeMedicationStatistics reconciliations(List<ReconciliationReportView> values) {
        reconciliations = List.copyOf(values);
        return this;
    }

    @Override
    public Stream<DoseReportView> dosesInPeriod(ScopeFilter scopeFilter, Instant from, Instant to) {
        return doses.stream();
    }

    @Override
    public Stream<ReconciliationReportView> reconciliationsInPeriod(ScopeFilter scopeFilter, Instant from, Instant to) {
        return reconciliations.stream();
    }
}
