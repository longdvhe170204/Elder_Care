package com.cambae.backend.m03.web.request;

import com.cambae.backend.m03.internal.structure.domain.StructureChange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** T017: thân lệnh theo hợp đồng Phase 3 đã duyệt. */
public record BedTransferReasonWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 30)
        String code,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 100)
        String name,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull
        Boolean medicalSafety) {
    public StructureChange change() {
        return StructureChange.transferReason(code, name, medicalSafety);
    }
}
