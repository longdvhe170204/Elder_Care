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
 * Chỉ tiêu {@code HEALTH.INCIDENT_RESPONSE_TIME} ({@code SK.SU_CO_THOI_GIAN_TIEP_NHAN}; FR-042, Q-199): thời gian trung
 * bình (phút) từ lúc tạo đến lúc tiếp nhận xử lý (chuyển Đang xử lý), chỉ sự cố đã tiếp nhận; sự cố chưa tiếp nhận đếm
 * ở {@code HEALTH.INCIDENTS}. Ô chia theo mức hiện hành: nguồn 007 chưa trả mức lúc tiếp nhận (plan 016, ghi chú
 * 2026-11-08).
 */
@Component
@Transactional(readOnly = true)
public class IncidentResponseTimeMetric implements MetricSource<IncidentRecord> {

    public static final String CODE = "HEALTH.INCIDENT_RESPONSE_TIME";

    private static final Metric<IncidentRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.HEALTH_EVENTS,
            "Thời gian tiếp nhận sự cố (phút)",
            "Trung bình từ lúc tạo đến lúc chuyển Đang xử lý, chỉ sự cố đã tiếp nhận, không gồm Đã hủy",
            "007 IncidentStatisticsApi.incidents",
            new Metric.Computation.Aggregate<>(
                    Metric.Computation.Aggregate.Kind.AVERAGE, IncidentRecord::responseMinutes),
            r -> r.view().level(),
            IncidentRecords::row);

    private final IncidentRecords records;

    public IncidentResponseTimeMetric(IncidentRecords records) {
        this.records = records;
    }

    @Override
    public Metric<IncidentRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<IncidentRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to).filter(IncidentRecord::acknowledged);
    }
}
