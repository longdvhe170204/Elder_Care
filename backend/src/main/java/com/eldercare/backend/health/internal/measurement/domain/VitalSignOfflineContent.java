package com.eldercare.backend.health.internal.measurement.domain;

import java.math.BigDecimal;
import java.util.Map;

/** Nội dung ghi chỉ số qua khung đồng bộ; mốc đo lấy từ phong bì thiết bị. */
public record VitalSignOfflineContent(
        long residentId,
        long indicatorId,
        Map<String, BigDecimal> values,
        boolean immediateAction,
        Long taskId,
        String note,
        Long originalRecordId) {
    public VitalSignOfflineContent {
        values = values != null ? Map.copyOf(values) : Map.of();
    }
}
