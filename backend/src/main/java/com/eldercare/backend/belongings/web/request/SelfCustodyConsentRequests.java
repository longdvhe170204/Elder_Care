package com.eldercare.backend.belongings.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh đồng ý cho tự giữ (api-013 {@code RecordSelfCustodyConsent}, rút, cổng người thân). */
public final class SelfCustodyConsentRequests {

    private SelfCustodyConsentRequests() {}

    public record Record(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long belongingId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long representativeId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long signedFormFileId) {}

    public record Withdraw(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String reason,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long representativeId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long signedFormFileId) {}

    public record PortalRecord(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long belongingId) {}

    public record PortalWithdraw(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String reason) {}
}
