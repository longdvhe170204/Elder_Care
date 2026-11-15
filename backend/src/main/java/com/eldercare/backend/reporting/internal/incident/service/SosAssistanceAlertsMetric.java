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
 * Chỉ tiêu {@code HEALTH.SOS_ASSISTANCE_ALERTS} ({@code SK.CANH_BAO_SOS_HO_TRO}; FR-041, Q-369, Q-374, Q-375; research
 * O-12, O-13): số cảnh báo từ nút SOS và nút hỗ trợ khẩn cấp, mỗi nguồn một ô theo tầng ({@code <nguồn>:<tầng>}, tầng
 * rỗng là {@code UNKNOWN_FLOOR}); cảnh báo nút hỗ trợ không có người cao tuổi dùng tầng của cảnh báo. Thời gian tiếp
 * nhận, tới nơi và số "không ghi mốc tới nơi" ở các mã {@code HEALTH.SOS_ASSISTANCE_*} cùng ô. Dòng chi tiết có phút tạo → tiếp nhận, tạo → tới nơi và dấu
 * "không ghi mốc tới nơi" của 007.
 */
@Component
@Transactional(readOnly = true)
public class SosAssistanceAlertsMetric implements MetricSource<AlertRecord> {

    public static final String CODE = "HEALTH.SOS_ASSISTANCE_ALERTS";

    private static final Metric<AlertRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.HEALTH_EVENTS,
            "Cảnh báo nút SOS, nút hỗ trợ",
            "Số cảnh báo từ nút SOS, nút hỗ trợ khẩn cấp có lần tạo đầu trong khoảng, chia theo nguồn và tầng; vẫn đếm trong"
                    + " tổng cảnh báo theo mức",
            "007 IncidentStatisticsApi.alerts (source, arrivedAt, arrivalTimeMissing)",
            new Metric.Computation.Count<>(AlertRecord::fromButton),
            AlertRecord::sourceAndFloor,
            AlertRecords::row);

    private final AlertRecords records;

    public SosAssistanceAlertsMetric(AlertRecords records) {
        this.records = records;
    }

    @Override
    public Metric<AlertRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<AlertRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.in(rough, from, to).filter(AlertRecord::fromButton);
    }
}
