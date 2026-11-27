package com.eldercare.backend.nutrition.internal.catalog.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

/**
 * Thuộc tính của món ăn ở {@code catalog_item} loại {@code M08.DISH} (data-model mục 13.1, Q-445): mô tả, ảnh (tệp đính kèm
 * {@code M08.DISH_IMAGE}). {@code status} là vòng đời của món (cột {@code status} của bảng chung chỉ có Hiệu lực/Ngừng: món
 * Ngừng hiệu lực hoặc Đã hủy là {@code INACTIVE}). Bỏ qua khóa cũ ({@code note}, {@code effectiveVersionId}) của món có
 * trước Q-445.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DishAttributes(
        DishStatus status,
        String description,
        Long imageFileId,
        boolean everActive,
        String deactivationReason,
        Long deactivatedBy,
        Instant deactivatedAt) {

    public static final SharedKind<DishAttributes> KIND =
            SharedKind.of("M08.DISH", DishAttributes.class).notDeletable();

    public static DishAttributes draft(String description) {
        return new DishAttributes(DishStatus.DRAFT, description, null, false, null, null, null);
    }

    public DishAttributes withStatus(DishStatus value) {
        return new DishAttributes(
                value, description, imageFileId, everActive, deactivationReason, deactivatedBy, deactivatedAt);
    }

    public DishAttributes withDescription(String value) {
        return new DishAttributes(
                status, value, imageFileId, everActive, deactivationReason, deactivatedBy, deactivatedAt);
    }

    public DishAttributes withImage(Long fileId) {
        return new DishAttributes(
                status, description, fileId, everActive, deactivationReason, deactivatedBy, deactivatedAt);
    }

    public DishAttributes activated() {
        return new DishAttributes(
                DishStatus.ACTIVE, description, imageFileId, true, deactivationReason, deactivatedBy, deactivatedAt);
    }

    public DishAttributes deactivated(String reason, long by, Instant at) {
        return new DishAttributes(DishStatus.INACTIVE, description, imageFileId, everActive, reason, by, at);
    }
}
