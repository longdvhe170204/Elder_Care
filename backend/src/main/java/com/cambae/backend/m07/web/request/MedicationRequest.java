package com.cambae.backend.m07.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** api-006 {@code Medication} khi thêm, sửa (FR-001, FR-002); {@code id}, {@code active} chỉ đọc, bỏ qua khi ghi. */
public record MedicationRequest(
        Long id,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
        String name,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
        String concentration,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
        String dosageForm,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
        String defaultRoute,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
        String usageUnit,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotEmpty
        List<Long> allergenIds,

        Integer beforeWindowMinutes,
        Integer afterWindowMinutes,
        Boolean specialControl,
        Boolean active) {}
