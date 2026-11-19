package com.eldercare.backend.care.web.response;

import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import io.swagger.v3.oas.annotations.media.Schema;

/** Nhóm sở thích của danh mục (FR-051; api-014 {@code /interest-groups}, thêm 2026-11-15). */
public record InterestGroupResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status) {
    public static InterestGroupResponse of(CatalogItem<Void> item) {
        return new InterestGroupResponse(item.id(), item.name(), item.status().name());
    }
}
