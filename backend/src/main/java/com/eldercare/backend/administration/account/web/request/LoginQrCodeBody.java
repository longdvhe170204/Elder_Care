package com.eldercare.backend.administration.account.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân {@code POST /resident-accounts/{residentId}/login-qr-code} (FR-069). */
public record LoginQrCodeBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UnlockResidentBody.ResidentConsent consent) {}
