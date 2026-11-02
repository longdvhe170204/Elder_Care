package com.eldercare.backend.roombedallocation.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** T017: thân lệnh theo hợp đồng Phase 3 đã duyệt. */
public record BedWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull
        Long roomId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 20)
        String code) {}
