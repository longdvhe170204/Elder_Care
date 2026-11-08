package com.eldercare.backend.reporting.internal.incident.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.incident.service.AlertRecords.AlertRecord;
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
 * Chỉ tiêu {@code HEALTH.ALERT_ESCALATIONS} ({@code SK.CANH_BAO_LEO_THANG}; FR-041, Q-266): tổng số lần leo thang của
 * cảnh báo tạo trong khoảng theo số 007 đếm (lần nhắc lại khi đã leo thang tối đa không tính), chia theo mức hiện hành;
 * dòng chi tiết là cảnh báo có ít nhất một lần leo thang.
 */
@Component
@Transactional(readOnly = true)
public class AlertEscalationsMetric implements MetricSource<AlertRecord> {

    public static final String CODE = "HEALTH.ALERT_ESCALATIONS";

    private static final Metric<AlertRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.HEALTH_EVENTS,
            "Số lần leo thang cảnh báo",
            "Tổng số lần leo thang của cảnh báo tạo trong khoảng, không gồm lần nhắc khi đã leo thang tối đa",
            "007 IncidentStatisticsApi.alerts",
            new Metric.Computation.Aggregate<>(
                    Metric.Computation.Aggregate.Kind.SUM,
                    r -> BigDecimal.valueOf(r.view().escalationCount())),
            r -> r.view().level(),
            AlertRecords::row);

    private final AlertRecords records;

    public AlertEscalationsMetric(AlertRecords records) {
        this.records = records;
    }

    @Override
    public Metric<AlertRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<AlertRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to).filter(r -> r.view().escalationCount() > 0);
    }
}
