package com.eldercare.backend.health.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Lần đo đã xác nhận cho thống kê, mức lúc đo theo thành phần nặng nhất (FR-081); {@code values} là giá trị hiện hành
 * theo thành phần (đã áp đính chính) cho chuỗi giá trị theo thời gian của một người (016 FR-040).
 */
public record VitalSignReportView(
        long recordId,
        long residentId,
        long indicatorId,
        Long floorAreaId,
        Instant measuredAt,
        VitalSignLevel level,
        Map<String, BigDecimal> values) {

    public VitalSignReportView {
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
