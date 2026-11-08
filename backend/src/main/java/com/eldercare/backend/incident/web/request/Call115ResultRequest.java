package com.eldercare.backend.incident.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân {@code POST /call-115-attempts/{id}/result} (FR-047k). */
public record Call115ResultRequest(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"CONNECTED", "NOT_CONNECTED"})
        String result) {}
