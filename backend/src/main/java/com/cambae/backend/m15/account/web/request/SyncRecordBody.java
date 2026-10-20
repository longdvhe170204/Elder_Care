package com.cambae.backend.m15.account.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SyncRecordBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        UUID idempotencyKey,

        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "M04.CARE_TASK_RESULT")
        String recordType,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Instant deviceTime,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Map<String, Object> content) {}
