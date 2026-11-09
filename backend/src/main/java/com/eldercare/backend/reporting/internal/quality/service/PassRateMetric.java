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
 * Chỉ tiêu {@code QC.PASS_RATE} ({@code KT.TY_LE_DAT}; FR-037): tỷ lệ Đạt theo nhân viên thực hiện công việc gốc; tầng ở bộ
 * lọc chung. Mục "ngoài phạm vi kiểm tra" (Q-168, Q-254) do 014 không lập nên không có ở nguồn.
 */
@Component
@Transactional(readOnly = true)
public class PassRateMetric implements MetricSource<QualityCheckRecord> {

    public static final String CODE = "QC.PASS_RATE";

    private static final Metric<QualityCheckRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.QUALITY_CHECKS,
            "Tỷ lệ Đạt kiểm tra chất lượng (%)",
            "Số Đạt ÷ (Đạt + Không đạt) theo nhân viên thực hiện công việc gốc; Không kiểm tra được, Quá hạn kiểm tra không vào mẫu số, xem ở dòng chi tiết",
            "014 ActivityStatisticsApi.qualityChecks",
            new Metric.Computation.Ratio<>(
                    QualityCheckRecord::passOrFail,
                    r -> QualityCheckRecords.PASS.equals(r.view().result())),
            QualityCheckRecord::performerKey,
            QualityCheckRecords::row);

    private final QualityCheckRecords records;

    public PassRateMetric(QualityCheckRecords records) {
        this.records = records;
    }

    @Override
    public Metric<QualityCheckRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<QualityCheckRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to);
    }
}
