package com.eldercare.backend.health.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Chỉ số gần nhất: giá trị hiện hành sau đính chính, thông tin danh mục và thời điểm đo (007 mục 1.2). */
public record LatestVitalSignView(
        long recordId,
        long indicatorId,
        String indicatorCode,
        String indicatorName,
        String unit,
        Map<String, BigDecimal> values,
        Instant measuredAt) {
    public LatestVitalSignView {
        values = Map.copyOf(values);
    }
}
