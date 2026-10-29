package com.eldercare.backend.administration.account.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Xác minh danh tính khi cấp lại mật khẩu người thân (FR-052, US6). */
@Schema(name = "Verification")
public record Verification(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) VerificationMethod method,

        String idDocumentNumber,
        String calledPhoneNumber) {}
