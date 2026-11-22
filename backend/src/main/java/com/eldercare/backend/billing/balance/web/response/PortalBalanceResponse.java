package com.eldercare.backend.billing.balance.web.response;

import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionView;
import com.eldercare.backend.billing.balance.internal.ledger.domain.PaymentInstructions;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import java.util.List;

/**
 * Schema {@code PortalBalance} của api-017 (FR-003): chỉ giao dịch Đã xác nhận; thanh toán bảng chỉ có tổng (Q-133).
 * Ghi chú nội bộ của Kế toán ({@code note}) không gửi cho người thân (chủ dự án chốt 2026-11-22, test BF-16).
 */
public record PortalBalanceResponse(
        long balance,
        long depositBalance,
        Long debt,
        List<BalanceTransactionView> transactions,
        PaymentInstructions paymentInstructions) {

    public static PortalBalanceResponse of(BalanceQueries.Portal portal) {
        var b = portal.balance();
        return new PortalBalanceResponse(
                b.balance(),
                b.depositBalance(),
                b.debt(),
                portal.transactions().stream()
                        .map(PortalBalanceResponse::withoutNote)
                        .toList(),
                b.paymentInstructions());
    }

    private static BalanceTransactionView withoutNote(BalanceTransactionView t) {
        return new BalanceTransactionView(
                t.id(),
                t.ledger(),
                t.type(),
                t.amount(),
                t.at(),
                t.method(),
                t.bankTransactionCode(),
                t.costStatementId(),
                t.originalTransactionId(),
                t.linkedTransactionId(),
                t.createdBy(),
                t.approvedBy(),
                t.reason(),
                null,
                t.status(),
                t.adjusted(),
                t.approvalRequestId());
    }
}
