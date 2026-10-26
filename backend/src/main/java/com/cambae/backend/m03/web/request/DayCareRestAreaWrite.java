package com.cambae.backend.m03.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** T052: đúng các trường cấu hình khu nghỉ đã duyệt. */
public record DayCareRestAreaWrite(
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        Long floorId,
        Long areaId) {}
