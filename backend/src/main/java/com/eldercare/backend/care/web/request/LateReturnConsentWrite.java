package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LateReturnConsent")
public record LateReturnConsentWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long residentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long sessionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long representativeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long signedFileId) {

    public record Portal(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long residentId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long sessionId) {}
}
