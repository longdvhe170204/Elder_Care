package com.cambae.backend.m03.web.request;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m03.internal.structure.domain.StructureChange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** T017: thân lệnh theo hợp đồng Phase 3 đã duyệt. */
public record AreaWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 20)
        String code,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 100)
        String name,

        @Schema(implementation = String.class) Reason reason) {
    public StructureChange change() {
        return StructureChange.area(code, name).withReason(reason);
    }
}
