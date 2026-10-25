package com.cambae.backend.m03.web.request;

import com.cambae.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** T056: bác sĩ là id tài khoản, không phải văn bản tự nhập. */
public record InfectionControlTransferWrite(
        @NotNull @Positive @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long newBedId,

        @NotNull @Positive @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long prescribingDoctorId,

        @Schema(implementation = String.class, requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {}
