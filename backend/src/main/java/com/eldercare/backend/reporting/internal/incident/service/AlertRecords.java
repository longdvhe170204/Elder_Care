package com.eldercare.backend.reporting.internal.incident.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.incident.api.IncidentStatisticsApi;
import com.eldercare.backend.incident.api.dto.AlertReportView;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cảnh báo cho {@code HEALTH.ALERT*}, {@code HEALTH.SOS_ASSISTANCE_ALERTS} (FR-041) đọc
 * {@code IncidentStatisticsApi.alerts} của 007: cảnh báo gộp theo DBR-18 là một dòng, mốc tạo là lần tạo đầu, mốc tiếp
 * nhận, mức lúc tiếp nhận và số lần leo thang (không gồm lần nhắc "đã leo thang tối đa", Q-266) do 007 xác định
 * (FR-081, SC-013).
 */
@Component
@Transactional(readOnly = true)
public class AlertRecords {

    /** Nguồn của cảnh báo từ nút SOS, nút hỗ trợ khẩn cấp (007 {@code AlertReportView.source}; research O-13). */
    static final String SOS = "SOS";

    static final String EMERGENCY_ASSIST = "EMERGENCY_ASSIST";

    /** Một cảnh báo; mốc xếp khoảng là lần tạo đầu. */
    public record AlertRecord(AlertReportView view) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M05.ALERT:" + view.alertId();
        }

        @Override
        public Instant referenceTime() {
            return view.firstAt();
        }

        @Override
        public Long floorAreaId() {
            return view.floorAreaId();
        }

        @Override
        public Long residentId() {
            return view.residentId();
        }

        boolean acknowledged() {
            return view.acknowledgedAt() != null;
        }

        /** Phút từ lúc tạo đến lúc tiếp nhận; rỗng khi chưa tiếp nhận. */
        BigDecimal responseMinutes() {
            return acknowledged() ? minutes(view.firstAt(), view.acknowledgedAt()) : null;
        }

        boolean fromButton() {
            return SOS.equals(view.source()) || EMERGENCY_ASSIST.equals(view.source());
        }
    }

    private final ObjectProvider<IncidentStatisticsApi> statistics;

    public AlertRecords(ObjectProvider<IncidentStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    public Stream<AlertRecord> in(ScopeFilter rough, Instant from, Instant to) {
        IncidentStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        return api.alerts(rough, from, to).map(AlertRecord::new);
    }

    static BigDecimal minutes(Instant from, Instant to) {
        return BigDecimal.valueOf(Duration.between(from, to).toSeconds())
                .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
    }

    static Long wholeMinutes(Instant from, Instant to) {
        return from == null || to == null ? null : Duration.between(from, to).toMinutes();
    }

    static DetailRow row(AlertRecord r) {
        AlertReportView v = r.view();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", v.type());
        result.put("source", v.source());
        result.put("levelAtAcknowledgement", v.levelAtAcknowledgement());
        result.put("acknowledgedAt", v.acknowledgedAt());
        result.put("responseMinutes", wholeMinutes(v.firstAt(), v.acknowledgedAt()));
        result.put("escalationCount", v.escalationCount());
        result.put("arrivedAt", v.arrivedAt());
        result.put("arrivalMinutes", wholeMinutes(v.firstAt(), v.arrivedAt()));
        result.put("arrivalTimeMissing", v.arrivalTimeMissing());
        return new DetailRow(
                r.sourceKey(),
                "007",
                "ALERT",
                v.level(),
                r.referenceTime(),
                r.floorAreaId(),
                null,
                r.residentId(),
                null,
                null,
                result,
                InfoGroup.HEALTH,
                Map.of(),
                false,
                false,
                false);
    }
}
