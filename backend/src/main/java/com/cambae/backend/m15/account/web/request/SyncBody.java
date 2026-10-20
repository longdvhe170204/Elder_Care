package com.cambae.backend.m15.account.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public record SyncBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Instant lastConnectedAt,

        @NotNull @Valid @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<SyncRecordBody> records) {}
