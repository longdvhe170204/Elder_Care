package com.cambae.backend.m03.web.request;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m03.api.dto.GenderPolicy;
import com.cambae.backend.m03.api.spi.CareLevelCode;
import com.cambae.backend.m03.internal.structure.domain.StructureChange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** T017: thân lệnh theo hợp đồng Phase 3 đã duyệt. */
public record RoomWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull
        Long floorId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 20)
        String code,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 100)
        String name,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull
        Long roomTypeId,

        @Size(min = 1) Set<CareLevelCode> allowedCareLevels,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull
        GenderPolicy genderPolicy,

        @Schema(implementation = String.class) Reason reason) {
    public StructureChange change() {
        return StructureChange.room(floorId, roomTypeId, code, name, genderPolicy, allowedCareLevels)
                .withReason(reason);
    }
}
