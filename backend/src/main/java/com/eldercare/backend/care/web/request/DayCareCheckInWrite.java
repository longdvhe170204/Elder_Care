package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record DayCareCheckInWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long residentId, Instant at) {}
