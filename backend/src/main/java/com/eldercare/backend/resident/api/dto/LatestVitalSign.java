package com.eldercare.backend.resident.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Chỉ số đo gần nhất của người cao tuổi, để Bác sĩ tham khảo khi đánh giá (FR-024). DTO riêng của M01, không dùng DTO của
 * health: giá trị hiện hành sau đính chính, kèm thời điểm đo gốc.
 */
public record LatestVitalSign(
        long indicatorId,
        String indicatorCode,
        String indicatorName,
        String unit,
        Map<String, BigDecimal> values,
        Instant measuredAt) {
    public LatestVitalSign {
        values = Map.copyOf(values);
    }
}
