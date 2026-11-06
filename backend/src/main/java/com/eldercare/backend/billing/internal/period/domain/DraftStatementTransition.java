package com.eldercare.backend.billing.internal.period.domain;

/** FR-002/003/004: quyết định dưới khóa của lệnh; không tự lập bảng/khoản hoặc khoản điều chỉnh. */
public record DraftStatementTransition(boolean createStatement, boolean reopenStatement, boolean requiresAdjustment) {
    public static DraftStatementTransition forNewDraft(String periodStatus, String currentStatus) {
        if ("FINALIZED".equals(periodStatus)) return new DraftStatementTransition(false, false, true);
        if (!"OPEN".equals(periodStatus) && !"AWAITING_FINALIZATION".equals(periodStatus)) {
            throw new IllegalArgumentException("Trạng thái kỳ chi phí không hợp lệ");
        }
        return forNewDraft(currentStatus);
    }

    public static DraftStatementTransition forNewDraft(String currentStatus) {
        if (currentStatus == null) return new DraftStatementTransition(true, false, false);
        return switch (currentStatus) {
            case "OPEN" -> new DraftStatementTransition(false, false, false);
            case "AWAITING_FINALIZATION" -> new DraftStatementTransition(false, true, false);
            case "FINALIZED" -> new DraftStatementTransition(false, false, true);
            default -> throw new IllegalArgumentException("Trạng thái bảng chi phí không hợp lệ");
        };
    }
}
