package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** 014 T012: thân {@code POST /activity-sessions/{id}/registrations} của api-014 (FR-015, FR-018). */
public record ActivityRegistrationWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long residentId, Boolean acknowledgeWarnings) {}
