package com.eldercare.backend.nutrition.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Thân tạo thực đơn Nháp (api-011 {@code POST /menus}; FR-023, FR-024). */
@Schema(name = "CreateMenu")
public record CreateMenuRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate weekStart, Long copyFromId) {}
