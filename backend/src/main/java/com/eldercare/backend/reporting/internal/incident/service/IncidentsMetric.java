package com.eldercare.backend.reporting.internal.incident.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.incident.service.IncidentRecords.IncidentRecord;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code HEALTH.INCIDENTS} ({@code SK.SU_CO}; FR-020, FR-042): số sự cố tạo trong khoảng, không gồm Đã hủy,
 * chia theo mức hiện hành; loại ở dòng chi tiết. Quản lý viện thấy cả sự cố "không thuộc sức khỏe" ở đây (FR-042a).
 */
@Component
@Transactional(readOnly = true)
public class IncidentsMetric implements MetricSource<IncidentRecord> {

    public static final String CODE = "HEALTH.INCIDENTS";

    private static final Metric<IncidentRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.HEALTH_EVENTS,
            "Sự cố",
            "Số sự cố tạo trong khoảng, không gồm sự cố Đã hủy, chia theo mức hiện hành",
            "007 IncidentStatisticsApi.incidents",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().level(),
            IncidentRecords::row);

    private final IncidentRecords records;

    public IncidentsMetric(IncidentRecords records) {
        this.records = records;
    }

    @Override
    public Metric<IncidentRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<IncidentRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to);
    }
}
