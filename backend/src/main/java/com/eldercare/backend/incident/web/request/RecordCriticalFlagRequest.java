package com.eldercare.backend.incident.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh ghi dấu nguy kịch (api-007 {@code POST /residents/{residentId}/critical-flags}; FR-050a). */
public record RecordCriticalFlagRequest(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"OFFICIAL", "PROVISIONAL"})
        String type,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
        String assessment) {}
