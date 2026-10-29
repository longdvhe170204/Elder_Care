package com.eldercare.backend.roombedallocation.web.request;

import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import com.eldercare.backend.roombedallocation.internal.structure.domain.StructureChange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** T017: thân lệnh theo hợp đồng Phase 3 đã duyệt. */
public record RoomTypeWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 100)
        String name,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull
        Integer standardBeds,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @Size(min = 1)
        Set<CareLevelCode> defaultCareLevels,

        @Schema(defaultValue = "true") Boolean servesLongTerm,
        @Schema(defaultValue = "true") Boolean servesShortTerm) {
    public StructureChange change() {
        return StructureChange.roomType(
                name,
                standardBeds,
                defaultCareLevels,
                servesLongTerm == null || servesLongTerm,
                servesShortTerm == null || servesShortTerm);
    }
}
