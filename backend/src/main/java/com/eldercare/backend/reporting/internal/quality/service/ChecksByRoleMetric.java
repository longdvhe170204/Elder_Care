package com.eldercare.backend.reporting.internal.quality.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.quality.service.QualityCheckRecords.QualityCheckRecord;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code QC.BY_CHECKER_ROLE} ({@code KT.THEO_VAI_TRO_KIEM_TRA}; FR-037, Q-254): số mục đã kiểm tra theo vai trò
 * kiểm tra.
 */
@Component
@Transactional(readOnly = true)
public class ChecksByRoleMetric implements MetricSource<QualityCheckRecord> {

    public static final String CODE = "QC.BY_CHECKER_ROLE";

    private static final Metric<QualityCheckRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.QUALITY_CHECKS,
            "Mục đã kiểm tra theo vai trò kiểm tra",
            "Số mục đã có kết quả Đạt, Không đạt, Không kiểm tra được, chia theo vai trò kiểm tra (Trưởng tầng, Người phụ trách ca)",
            "014 ActivityStatisticsApi.qualityChecks (checkerRole)",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().checkerRole() == null ? "UNKNOWN" : r.view().checkerRole(),
            QualityCheckRecords::row);

    private final QualityCheckRecords records;

    public ChecksByRoleMetric(QualityCheckRecords records) {
        this.records = records;
    }

    @Override
    public Metric<QualityCheckRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<QualityCheckRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to)
                .filter(r -> QualityCheckRecords.CHECKED.contains(r.view().result()));
    }
}
