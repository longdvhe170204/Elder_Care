package com.eldercare.backend.health.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/** Bản đọc chỉ số trong khoảng; giá trị hiện hành sau đính chính (007 mục 1.2). */
public record VitalSignReadingView(
        long id,
        long residentId,
        long indicatorId,
        Map<String, BigDecimal> values,
        VitalSignLevel level,
        VitalSignConfirmationStatus confirmationStatus,
        Instant measuredAt,
        Set<VitalSignLabel> labels) {
    public VitalSignReadingView {
        values = Map.copyOf(values);
        labels = Set.copyOf(labels);
    }
}
