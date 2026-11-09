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
 * Chỉ tiêu {@code HEALTH.ALERT_RESPONSE_TIME} ({@code SK.CANH_BAO_THOI_GIAN_TIEP_NHAN}; FR-041, 007 SC-013): thời gian
 * trung bình (phút) từ lần tạo đầu đến mốc tiếp nhận do 007 xác định, chỉ cảnh báo đã tiếp nhận, chia theo mức lúc tiếp
 * nhận. Cảnh báo chưa tiếp nhận không vào phép tính, đếm ở {@code HEALTH.ALERTS}.
 */
@Component
@Transactional(readOnly = true)
public class AlertResponseTimeMetric implements MetricSource<AlertRecord> {

    public static final String CODE = "HEALTH.ALERT_RESPONSE_TIME";

    private static final Metric<AlertRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.HEALTH_EVENTS,
            "Thời gian tiếp nhận cảnh báo (phút)",
            "Trung bình từ lần tạo đầu đến lúc tiếp nhận, chỉ cảnh báo đã tiếp nhận, chia theo mức lúc tiếp nhận",
            "007 IncidentStatisticsApi.alerts",
            new Metric.Computation.Aggregate<>(Metric.Computation.Aggregate.Kind.AVERAGE, AlertRecord::responseMinutes),
            r -> r.view().levelAtAcknowledgement(),
            AlertRecords::row);

    private final AlertRecords records;

    public AlertResponseTimeMetric(AlertRecords records) {
        this.records = records;
    }

    @Override
    public Metric<AlertRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<AlertRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to).filter(AlertRecord::acknowledged);
    }
}
