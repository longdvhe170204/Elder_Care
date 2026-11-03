package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record DayCareCheckOutWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long pickupRecordId, Instant at) {}
