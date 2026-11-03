package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record CareNotPerformedWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long reasonTypeId, String note) {}
