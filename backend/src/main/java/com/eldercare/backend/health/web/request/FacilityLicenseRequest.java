package com.eldercare.backend.health.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** Thân {@code POST /facility-licenses} (schema {@code FacilityLicense} của api-007). */
public record FacilityLicenseRequest(
        @Schema(description = "Bỏ qua khi lập; máy chủ cấp") Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String number,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String issuer,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate issuedOn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate expiresOn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Boolean coversMedicalPractice,
        List<String> scopes,
        Long scanFileId,
        Long replaces,
        Boolean replacesDueToError,
        String reason) {}
