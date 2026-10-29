package com.eldercare.backend.reporting.web.response;

import com.eldercare.backend.reporting.internal.detail.service.MetricDetailService.MetricDetail;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** {@code MetricDetail} của contracts/api-016.yaml (FR-009, FR-012, FR-014; dòng rút gọn Q-196). */
public record MetricDetailBody(String metricCode, int total, List<RowBody> rows) {

    public record RowBody(
            String sourceKey,
            String feature,
            String type,
            String status,
            Instant referenceTime,
            String floorArea,
            String resident,
            boolean reduced,
            String performedBy,
            Map<String, Object> result,
            boolean pendingReview,
            boolean canOpenRecord) {

        static RowBody of(DetailRow r) {
            return new RowBody(
                    r.sourceKey(),
                    r.feature(),
                    r.type(),
                    r.status(),
                    r.referenceTime(),
                    r.floorArea(),
                    r.resident(),
                    r.reduced(),
                    r.performedBy(),
                    r.result(),
                    r.pendingReview(),
                    r.canOpenRecord());
        }
    }

    public static MetricDetailBody of(MetricDetail d) {
        return new MetricDetailBody(
                d.metricCode(), d.total(), d.rows().stream().map(RowBody::of).toList());
    }
}
