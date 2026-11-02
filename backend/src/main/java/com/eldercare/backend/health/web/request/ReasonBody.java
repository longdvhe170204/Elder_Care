package com.eldercare.backend.health.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Lý do bắt buộc của lệnh Ngừng hiệu lực. */
@Schema(name = "M06ReasonBody")
public record ReasonBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {}
