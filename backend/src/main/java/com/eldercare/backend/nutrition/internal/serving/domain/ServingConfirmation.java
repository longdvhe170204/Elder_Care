package com.eldercare.backend.nutrition.internal.serving.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Xác nhận phục vụ suất đặc biệt (FR-049 → FR-051, BR-M08-14; research M-10; {@code M08.SERVING_CONFIRMATION}, nhóm 3,
 * đặt một lần). Cha là suất đặc biệt (hoặc suất có dấu cần đối chiếu); {@code residentId} là người được phục vụ.
 *
 * @param incidentId sự cố 007 ghi khi khai "đã ăn" ({@code null} ở các kết quả khác)
 * @param alertSource khóa nguồn đã gửi 007 khi xung đột dị ứng ({@code null} ở kết quả đúng suất)
 */
public record ServingConfirmation(
        long specialPortionId,
        long dailyMealId,
        long residentId,
        long floorAreaId,
        Result result,
        Long incidentId,
        String alertSource) {

    public static final SharedKind<ServingConfirmation> KIND =
            SharedKind.of("M08.SERVING_CONFIRMATION", ServingConfirmation.class);

    /** Kết quả (data-model mục 8): {@code DUNG_SUAT}, {@code CHAN_XUNG_DOT_DA_AN}, {@code CHAN_XUNG_DOT_CHUA_AN}. */
    public enum Result {
        CORRECT_PORTION,
        BLOCKED_CONFLICT_EATEN,
        BLOCKED_CONFLICT_NOT_EATEN
    }
}
