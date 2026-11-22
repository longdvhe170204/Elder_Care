package com.eldercare.backend.medication.internal.dose.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Phần của liều thêm khi sửa BF-07 (Q-426 → Q-428): dấu Hoãn, người nhận liều, mức quan trọng của đơn, chỉ số cần đo và
 * đã đo, dấu dùng muộn. Tính khi xem, không là cột của liều tính từ đơn (Q-306).
 */
public record DoseExtras(
        boolean postponed,
        String postponedReason,
        Long claimedBy,
        Instant claimExpiresAt,
        String timeCriticality,
        List<String> requiredMeasurements,
        boolean late,
        List<MeasurementValue> measurements,
        Long resultId) {

    public record MeasurementValue(String code, BigDecimal value) {}

    public static final DoseExtras NONE =
            new DoseExtras(false, null, null, null, "NORMAL", List.of(), false, List.of(), null);
}
