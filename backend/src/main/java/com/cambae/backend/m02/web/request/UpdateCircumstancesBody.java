package com.cambae.backend.m02.web.request;

import com.cambae.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Thân Cập nhật tình huống đặc biệt (contracts/api-004.yaml, POST {@code /waiting-list-entries/{id}/update-circumstances}):
 * toàn bộ tình huống sau khi sửa cộng lý do (FR-007).
 */
public record UpdateCircumstancesBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> circumstances,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {}
