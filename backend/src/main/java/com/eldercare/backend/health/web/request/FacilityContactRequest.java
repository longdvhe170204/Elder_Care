package com.eldercare.backend.health.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân {@code PUT /facility-contact}. */
public record FacilityContactRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String phoneNumber, String note) {}
