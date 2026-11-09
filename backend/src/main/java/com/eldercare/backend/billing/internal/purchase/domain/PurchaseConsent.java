package com.eldercare.backend.billing.internal.purchase.domain;

/** FR-023/024, Q-259/269: máy trạng thái; số được phép chỉ đổi khi đồng ý. */
public record PurchaseConsent(
        String status, long estimatedAmount, Long allowedAmount, Long threshold, Long additionalAmount) {
    public static PurchaseConsent draft(long amount) {
        if (amount < 0 || amount > 999999999999999L) throw new IllegalArgumentException("Số tiền không hợp lệ");
        return new PurchaseConsent("DRAFT", amount, null, null, null);
    }

    public boolean additionalPending() {
        return additionalAmount != null;
    }

    public boolean terminal() {
        return java.util.Set.of("REJECTED", "CANCELLED", "PURCHASED").contains(status);
    }

    public PurchaseConsent send(long limit) {
        require("DRAFT");
        if (limit < 0) throw new IllegalArgumentException("Ngưỡng không hợp lệ");
        return new PurchaseConsent(
                estimatedAmount <= limit ? "PURCHASE_ALLOWED" : "AWAITING_CONSENT",
                estimatedAmount,
                estimatedAmount <= limit ? estimatedAmount : null,
                limit,
                null);
    }

    public PurchaseConsent agree() {
        require("AWAITING_CONSENT");
        return new PurchaseConsent(
                "PURCHASE_ALLOWED",
                estimatedAmount,
                additionalPending() ? additionalAmount : estimatedAmount,
                threshold,
                null);
    }

    public PurchaseConsent reject() {
        require("AWAITING_CONSENT");
        return new PurchaseConsent(
                additionalPending() ? "PURCHASE_ALLOWED" : "REJECTED", estimatedAmount, allowedAmount, threshold, null);
    }

    public PurchaseConsent requestAdditional(long amount) {
        require("PURCHASE_ALLOWED");
        if (amount <= allowedAmount || amount > 999999999999999L)
            throw new IllegalArgumentException("Số mới phải lớn hơn số được phép và trong giới hạn lưu tiền");
        return new PurchaseConsent("AWAITING_CONSENT", estimatedAmount, allowedAmount, threshold, amount);
    }

    public PurchaseConsent withdraw() {
        require("AWAITING_CONSENT");
        if (!additionalPending()) throw new IllegalStateException("Không có yêu cầu bổ sung đang chờ");
        return new PurchaseConsent("PURCHASE_ALLOWED", estimatedAmount, allowedAmount, threshold, null);
    }

    public PurchaseConsent cancel() {
        if (terminal()) throw new IllegalStateException("Đề nghị đã kết thúc");
        return new PurchaseConsent("CANCELLED", estimatedAmount, allowedAmount, threshold, null);
    }

    public PurchaseConsent purchase() {
        require("PURCHASE_ALLOWED");
        return new PurchaseConsent("PURCHASED", estimatedAmount, allowedAmount, threshold, null);
    }

    private void require(String expected) {
        if (!expected.equals(status))
            throw new IllegalStateException("Đề nghị không còn ở trạng thái cho phép lệnh này");
    }
}
