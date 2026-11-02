package com.eldercare.backend.reporting.web.response;

import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ValueLabel;
import java.math.BigDecimal;
import java.util.List;

/** {@code MetricValue} của contracts/api-016.yaml: tỷ lệ luôn kèm tử số, mẫu số; mẫu số 0 thì {@code value} rỗng (FR-008). */
public record MetricValueBody(
        String metricCode,
        BigDecimal value,
        BigDecimal numerator,
        BigDecimal denominator,
        List<ValueLabel> labels,
        int pendingReviewCount,
        List<BreakdownBody> breakdown) {

    public record BreakdownBody(String key, BigDecimal value, BigDecimal numerator, BigDecimal denominator) {}

    public static MetricValueBody of(MetricValue v) {
        return new MetricValueBody(
                v.metricCode(),
                v.value(),
                v.numerator(),
                v.denominator(),
                v.labels().stream().sorted().toList(),
                v.pendingReviewCount(),
                v.breakdown().stream()
                        .map(b -> new BreakdownBody(b.key(), b.value(), b.numerator(), b.denominator()))
                        .toList());
    }
}
