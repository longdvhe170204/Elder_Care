package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds;
import io.swagger.v3.oas.annotations.media.Schema;

/** 019 (FR-011): phần tử {@code AssetType} của api-019 GET /asset-types; {@code code} chỉ có ở loại hệ thống BED, VEHICLE. */
@Schema(name = "AssetType")
public record AssetTypeResponse(
        Long id,
        String code,
        String name,
        @Schema(allowableValues = {"ACTIVE", "INACTIVE"}) String status) {

    public static AssetTypeResponse of(CatalogItem<AssetKinds.TypeAttributes> item) {
        return new AssetTypeResponse(
                item.id(), item.code(), item.name(), item.status().name());
    }
}
