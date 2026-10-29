package com.eldercare.backend.roombedallocation.web.request;

import com.eldercare.backend.roombedallocation.internal.structure.domain.StructureChange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** T017: thân lệnh theo hợp đồng Phase 3 đã duyệt. */
public record FloorWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull
        Long areaId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 20)
        String code,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 100)
        String name,

        @Size(max = 100) String buildingName) {
    public StructureChange change() {
        return StructureChange.floor(areaId, code, name, buildingName);
    }
}
