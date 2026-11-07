package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.incident.internal.alert.domain.SosPlace;
import io.swagger.v3.oas.annotations.media.Schema;

/** Thân {@code POST /sos/{id}/place} (FR-047f). */
public record SosPlaceRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SosPlace place) {}
