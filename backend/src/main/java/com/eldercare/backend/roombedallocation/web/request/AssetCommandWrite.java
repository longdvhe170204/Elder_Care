package com.eldercare.backend.roombedallocation.web.request;

import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetCommandInput;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** 019 T018: thân {@code AssetCommand} của api-019 (glossary 25.3); trường dùng theo từng lệnh. */
public record AssetCommandWrite(
        String reason,
        Location location,
        String description,
        @Schema(allowableValues = {"UNSAFE", "STILL_SAFE"}) String impactLevel,
        @Schema(allowableValues = {"PASS", "FAIL"}) String result,
        LocalDate date,
        String content,
        String performedBy,
        Boolean outsourced,
        Long cost,
        Long procurementRequestId,
        String repairDescription,
        String repairedBy,
        Long newDepartmentId,
        Long damageId) {

    @Schema(name = "AssetLocationRef")
    public record Location(
            @Schema(allowableValues = {"AREA", "FLOOR", "ROOM", "PARKING_LOT", "ASSET_STORAGE"})
            String type,

            Long id) {}

    public AssetCommandInput input() {
        return new AssetCommandInput(
                reason,
                location == null ? null : location.type(),
                location == null ? null : location.id(),
                description,
                impactLevel,
                result,
                date,
                content,
                performedBy,
                outsourced,
                cost,
                procurementRequestId,
                newDepartmentId);
    }
}
