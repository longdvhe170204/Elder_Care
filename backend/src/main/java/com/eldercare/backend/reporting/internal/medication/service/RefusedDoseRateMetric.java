package com.eldercare.backend.reporting.internal.medication.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.medication.api.dto.DoseStatus;
import com.eldercare.backend.reporting.internal.medication.service.DoseRecords.DoseRecord;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code MED.REFUSED_RATE} ({@code THUOC.TY_LE_TU_CHOI}; FR-043, 006 FR-051): số liều Từ chối ÷ số liều có trạng thái cuối trong mẫu số
 * ({@link DoseRecords}), thời điểm dự kiến trong khoảng; chia theo ca, lọc được theo người cao tuổi.
 */
@Component
@Transactional(readOnly = true)
public class RefusedDoseRateMetric implements MetricSource<DoseRecord> {

    public static final String CODE = "MED.REFUSED_RATE";

    private static final Metric<DoseRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.MEDICATION_DOSES,
            "Tỷ lệ liều Từ chối (%)",
            "Số liều Từ chối ÷ số liều có trạng thái cuối Đã dùng, Từ chối, Không thực hiện hoặc Bỏ lỡ, thời điểm dự kiến"
                    + " trong khoảng; không gồm liều Đã hủy, Tạm dừng, Không thực hiện do hệ thống đóng",
            "006 MedicationStatisticsApi.dosesInPeriod",
            new Metric.Computation.Ratio<>(
                    DoseRecord::inDenominator, r -> r.view().finalStatus() == DoseStatus.REFUSED),
            DoseRecord::shiftKey,
            DoseRecords::row);

    private final DoseRecords records;

    public RefusedDoseRateMetric(DoseRecords records) {
        this.records = records;
    }

    @Override
    public Metric<DoseRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<DoseRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to);
    }
}
