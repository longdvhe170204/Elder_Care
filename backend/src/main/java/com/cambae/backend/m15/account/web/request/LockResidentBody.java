package com.cambae.backend.m15.account.web.request;

import com.cambae.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** Thân {@code POST /accounts/{id}/lock-resident} (FR-072; research A-16). */
public record LockResidentBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Basis basis,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {

    /** Căn cứ khóa: người cao tuổi tự yêu cầu, hoặc có lý do (glossary mục 7.3). */
    public enum Basis {
        RESIDENT_REQUESTED,
        WITH_REASON
    }
}
