package com.eldercare.backend.belongings.internal.catalog.domain;

import com.eldercare.backend.common.data.CatalogStatus;

/** Kiểu đọc danh mục M12 trả cho web (api-013 {@code BelongingType}, mức tình trạng, vị trí). */
public final class BelongingCatalogViews {

    private BelongingCatalogViews() {}

    /** {@code valuable} tính theo CFG-M12-01 hiện hành (FR-009). */
    public record BelongingTypeView(
            long id,
            String name,
            boolean hasCashAmount,
            boolean requiresConsentForUse,
            boolean valuable,
            CatalogStatus status,
            int version) {}

    public record ConditionLevelView(long id, String name, int order, boolean unusable, CatalogStatus status) {}

    public record StorageLocationView(
            long id, String name, String description, Long floorAreaId, CatalogStatus status) {}
}
