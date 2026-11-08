package com.eldercare.backend.billing.balance.web.response;

import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionView;
import com.eldercare.backend.billing.balance.internal.ledger.domain.PaymentInstructions;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import java.util.List;

/** Schema {@code PortalBalance} của api-017 (FR-003): chỉ giao dịch Đã xác nhận; thanh toán bảng chỉ có tổng (Q-133). */
public record PortalBalanceResponse(
        long balance,
        long depositBalance,
        Long debt,
        List<BalanceTransactionView> transactions,
        PaymentInstructions paymentInstructions) {

    public static PortalBalanceResponse of(BalanceQueries.Portal portal) {
        var b = portal.balance();
        return new PortalBalanceResponse(
                b.balance(), b.depositBalance(), b.debt(), portal.transactions(), b.paymentInstructions());
    }
}
