package com.eldercare.backend.resident.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Thân lệnh chỉ cần lý do (contracts/api-001.yaml, requestBodies {@code Reason}). */
public record ReasonBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {}
