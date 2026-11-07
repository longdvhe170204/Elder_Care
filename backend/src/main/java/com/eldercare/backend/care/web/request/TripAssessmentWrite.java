package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record TripAssessmentWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long residentId,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"PASS", "FAIL"})
        String result,

        String reason) {}
