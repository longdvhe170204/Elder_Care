package com.eldercare.backend.incident.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Gắn thêm một sự cố lây nhiễm vào vùng. */
public record AttachIsolationIncidentRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long incidentId) {}
