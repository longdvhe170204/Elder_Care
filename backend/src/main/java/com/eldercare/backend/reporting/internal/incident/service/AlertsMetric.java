package com.eldercare.backend.reporting.internal.incident.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.incident.service.AlertRecords.AlertRecord;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code HEALTH.ALERTS} ({@code SK.CANH_BAO}; FR-020, FR-041): số cảnh báo tạo lần đầu trong khoảng, chia theo
 * mức hiện hành; cảnh báo từ nút SOS, nút hỗ trợ vẫn đếm trong tổng. Loại, mốc tiếp nhận ở dòng chi tiết.
 */
@Component
@Transactional(readOnly = true)
public class AlertsMetric implements MetricSource<AlertRecord> {

    public static final String CODE = "HEALTH.ALERTS";

    private static final Metric<AlertRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.HEALTH_EVENTS,
            "Cảnh báo",
            "Số cảnh báo có lần tạo đầu trong khoảng (cảnh báo gộp đếm một), chia theo mức hiện hành",
            "007 IncidentStatisticsApi.alerts",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().level(),
            AlertRecords::row);

    private final AlertRecords records;

    public AlertsMetric(AlertRecords records) {
        this.records = records;
    }

    @Override
    public Metric<AlertRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<AlertRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to);
    }
}
