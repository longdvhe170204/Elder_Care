package com.cambae.backend.m01.web.request;

import com.cambae.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Thân lệnh rút lại đồng ý (contracts/api-001.yaml, POST {@code /consents/{id}/withdraw}; FR-014): lý do và bản yêu cầu
 * rút lại đã scan.
 */
public record WithdrawConsentBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long evidenceFileId) {}
