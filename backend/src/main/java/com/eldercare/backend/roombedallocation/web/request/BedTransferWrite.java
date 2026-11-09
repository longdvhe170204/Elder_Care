package com.eldercare.backend.roombedallocation.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.Instant;

/** T036: đúng các trường của thân BedTransfer đã dịch. */
public record BedTransferWrite(
        @NotNull @Positive @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long newBedId,

        @NotNull @Positive @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long transferReasonId,

        @Schema(implementation = String.class, requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason,

        Instant transferAt) {}
