package com.eldercare.backend.administration.account.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân {@code POST /accounts/{id}/unlock-resident} (FR-073). */
public record UnlockResidentBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ResidentConsent consent) {

    /** Người cao tuổi yêu cầu hoặc đồng ý trực tiếp với nhân viên nào (contracts {@code ResidentConsent}). */
    @Schema(name = "ResidentConsent")
    public record ResidentConsent(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Method method,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long withStaffId) {}

    /** Cách xác nhận (glossary mục 7.3). */
    public enum Method {
        REQUESTED,
        AGREED
    }
}
