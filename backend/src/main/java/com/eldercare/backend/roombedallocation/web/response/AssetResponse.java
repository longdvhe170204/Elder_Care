package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.roombedallocation.api.dto.AssetStatus;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 019 T018: phản hồi {@code Asset} của api-019; {@code location} là nhãn vị trí hiện hành, {@code flags} là dấu. */
@Schema(name = "Asset")
public record AssetResponse(
        Long id,
        String code,
        Long typeId,
        String name,
        Long departmentId,
        AssetStatus status,
        String location,
        Long bedId,
        List<String> flags) {

    public static AssetResponse of(AssetRow row, String location) {
        return new AssetResponse(
                row.id(),
                row.code(),
                row.typeId(),
                row.name(),
                row.departmentId(),
                row.status(),
                location,
                row.bedId(),
                row.flags().stream().map(Enum::name).toList());
    }
}
