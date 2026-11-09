package com.eldercare.backend.nutrition.internal.catalog.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Thuộc tính của món ăn ở {@code catalog_item} loại {@code M08.DISH} (data-model mục 0.1, 3; R-15). {@code status} là
 * vòng đời của món (cột {@code status} của bảng chung chỉ có Hiệu lực/Ngừng: món Ngừng hiệu lực hoặc Đã hủy là
 * {@code INACTIVE}); {@code effectiveVersionId} trỏ phiên bản nội dung đã xác nhận đang dùng (M-04).
 */
public record DishAttributes(
        DishStatus status,
        String note,
        Long effectiveVersionId,
        boolean everActive,
        String deactivationReason,
        Long deactivatedBy,
        Instant deactivatedAt) {

    public static final SharedKind<DishAttributes> KIND =
            SharedKind.of("M08.DISH", DishAttributes.class).notDeletable();

    public DishAttributes withStatus(DishStatus value) {
        return new DishAttributes(
                value, note, effectiveVersionId, everActive, deactivationReason, deactivatedBy, deactivatedAt);
    }

    public DishAttributes withNote(String value) {
        return new DishAttributes(
                status, value, effectiveVersionId, everActive, deactivationReason, deactivatedBy, deactivatedAt);
    }

    public DishAttributes activatedWith(long versionId) {
        return new DishAttributes(
                DishStatus.ACTIVE, note, versionId, true, deactivationReason, deactivatedBy, deactivatedAt);
    }

    public DishAttributes deactivated(String reason, long by, Instant at) {
        return new DishAttributes(DishStatus.INACTIVE, note, effectiveVersionId, everActive, reason, by, at);
    }
}
