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
 * Chỉ tiêu {@code HEALTH.SOS_ASSISTANCE_RESPONSE_TIME_MAX} (phần của {@code SK.CANH_BAO_SOS_HO_TRO}; FR-041, Q-374, Q-375): tạo → tiếp nhận lớn nhất (research O-12); ô theo nguồn và
 * tầng như {@link SosAssistanceAlertsMetric}.
 */
@Component
@Transactional(readOnly = true)
public class SosAssistanceResponseTimeMaxMetric implements MetricSource<AlertRecord> {

    public static final String CODE = "HEALTH.SOS_ASSISTANCE_RESPONSE_TIME_MAX";

    private static final Metric<AlertRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.HEALTH_EVENTS,
            "Thời gian tiếp nhận cảnh báo nút SOS, nút hỗ trợ, lớn nhất (phút)",
            "Lớn nhất từ lần tạo đầu đến lúc tiếp nhận của cảnh báo nút SOS, nút hỗ trợ khẩn cấp đã tiếp nhận, theo nguồn và tầng",
            "007 IncidentStatisticsApi.alerts (source, acknowledgedAt, arrivedAt, arrivalTimeMissing)",
            new Metric.Computation.Aggregate<>(Metric.Computation.Aggregate.Kind.MAXIMUM, AlertRecord::responseMinutes),
            AlertRecord::sourceAndFloor,
            AlertRecords::row);

    private final AlertRecords records;

    public SosAssistanceResponseTimeMaxMetric(AlertRecords records) {
        this.records = records;
    }

    @Override
    public Metric<AlertRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<AlertRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to).filter(AlertRecord::fromButton).filter(AlertRecord::acknowledged);
    }
}
