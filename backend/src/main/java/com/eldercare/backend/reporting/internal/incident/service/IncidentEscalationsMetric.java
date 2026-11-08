package com.eldercare.backend.reporting.internal.incident.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.incident.service.IncidentRecords.IncidentRecord;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code HEALTH.INCIDENT_ESCALATIONS} ({@code SK.SU_CO_LEO_THANG}; FR-042): tổng số lần leo thang của sự cố
 * (sự cố Mới quá hạn tiếp nhận, 007) tạo trong khoảng, tính riêng với cảnh báo, chia theo mức hiện hành; dòng chi tiết là
 * sự cố có ít nhất một lần leo thang.
 */
@Component
@Transactional(readOnly = true)
public class IncidentEscalationsMetric implements MetricSource<IncidentRecord> {

    public static final String CODE = "HEALTH.INCIDENT_ESCALATIONS";

    private static final Metric<IncidentRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.HEALTH_EVENTS,
            "Số lần leo thang sự cố",
            "Tổng số lần leo thang của sự cố tạo trong khoảng (Mới quá hạn tiếp nhận), không gồm Đã hủy",
            "007 IncidentStatisticsApi.incidents",
            new Metric.Computation.Aggregate<>(
                    Metric.Computation.Aggregate.Kind.SUM,
                    r -> BigDecimal.valueOf(r.view().escalationCount())),
            r -> r.view().level(),
            IncidentRecords::row);

    private final IncidentRecords records;

    public IncidentEscalationsMetric(IncidentRecords records) {
        this.records = records;
    }

    @Override
    public Metric<IncidentRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<IncidentRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to).filter(r -> r.view().escalationCount() > 0);
    }
}
