package com.eldercare.backend.reporting.internal.incident.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.incident.api.IncidentStatisticsApi;
import com.eldercare.backend.incident.api.dto.IncidentReportView;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sự cố cho {@code HEALTH.INCIDENT*}, {@code HEALTH.NON_HEALTH_INCIDENTS} (FR-042, FR-042a) đọc
 * {@code IncidentStatisticsApi.incidents} của 007: sự cố Đã hủy không có trong nguồn (FR-007); mức, loại hiện hành, dấu
 * "không thuộc sức khỏe" theo danh mục hiện hành (Q-73, Q-99), mốc tiếp nhận (chuyển Đang xử lý) và số lần leo thang do
 * 007 xác định (FR-081).
 */
@Component
@Transactional(readOnly = true)
public class IncidentRecords {

    /** Một sự cố; mốc xếp khoảng là lúc tạo. */
    public record IncidentRecord(IncidentReportView view) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M05.INCIDENT:" + view.incidentId();
        }

        @Override
        public Instant referenceTime() {
            return view.createdAt();
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

        BigDecimal responseMinutes() {
            return acknowledged() ? AlertRecords.minutes(view.createdAt(), view.acknowledgedAt()) : null;
        }
    }

    private final ObjectProvider<IncidentStatisticsApi> statistics;

    public IncidentRecords(ObjectProvider<IncidentStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    /**
     * Số sự cố Đã hủy tạo trong khoảng, dòng tham khảo của FR-042, FR-007 (không vào chỉ tiêu nào; 016 T018, chủ dự án duyệt
     * 2026-11-12). Phạm vi là bộ lọc thô của nhóm, vì 007 chỉ trả số đếm.
     */
    public int cancelledCount(ScopeFilter rough, Instant from, Instant to) {
        IncidentStatisticsApi api = statistics.getIfAvailable();
        return api == null ? 0 : api.cancelledCount(rough, from, to);
    }

    public Stream<IncidentRecord> in(ScopeFilter rough, Instant from, Instant to) {
        IncidentStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        return api.incidents(rough, from, to).map(IncidentRecord::new);
    }

    static DetailRow row(IncidentRecord r) {
        IncidentReportView v = r.view();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("acknowledgedAt", v.acknowledgedAt());
        result.put("responseMinutes", AlertRecords.wholeMinutes(v.createdAt(), v.acknowledgedAt()));
        result.put("escalationCount", v.escalationCount());
        return new DetailRow(
                r.sourceKey(),
                "007",
                v.type(),
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
