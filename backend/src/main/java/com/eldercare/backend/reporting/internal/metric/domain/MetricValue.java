package com.eldercare.backend.reporting.internal.metric.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Giá trị một chỉ tiêu ({@code GiaTriChiTieu}; contracts {@code MetricValue}). Tỷ lệ là phần trăm một chữ số thập phân,
 * luôn kèm tử số, mẫu số (FR-008); mẫu số 0 thì {@code value} rỗng và nhãn {@link ValueLabel#NO_DATA}.
 */
public record MetricValue(
        String metricCode,
        BigDecimal value,
        BigDecimal numerator,
        BigDecimal denominator,
        Set<ValueLabel> labels,
        int pendingReviewCount,
        List<Breakdown> breakdown) {

    public record Breakdown(String key, BigDecimal value, BigDecimal numerator, BigDecimal denominator) {}
}
