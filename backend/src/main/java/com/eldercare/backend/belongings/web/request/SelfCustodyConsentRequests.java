package com.eldercare.backend.belongings.web.request;

/** Thân lệnh đồng ý cho tự giữ (api-013 {@code RecordSelfCustodyConsent}, rút, cổng người thân). */
public final class SelfCustodyConsentRequests {

    private SelfCustodyConsentRequests() {}

    public record Record(Long belongingId, Long representativeId, Long signedFormFileId) {}

    public record Withdraw(String reason, Long representativeId, Long signedFormFileId) {}

    public record PortalRecord(Long belongingId) {}

    public record PortalWithdraw(String reason) {}
}
