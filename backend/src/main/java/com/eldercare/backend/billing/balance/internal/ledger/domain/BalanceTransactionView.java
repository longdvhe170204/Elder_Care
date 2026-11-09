package com.eldercare.backend.billing.balance.internal.ledger.domain;

import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import java.time.Instant;

/**
 * Giao dịch theo schema {@code BalanceTransaction} của api-017 (glossary 23.2): dùng cho phản hồi và cho thân lỗi
 * {@code DuplicateCodeProblem}. {@code createdBy}, {@code approvedBy} là mã tài khoản dạng chuỗi; {@code SYSTEM} với
 * giao dịch lập kèm lệnh chốt bảng (FR-012).
 */
public record BalanceTransactionView(
        long id,
        Ledger ledger,
        TransactionType type,
        long amount,
        Instant at,
        PaymentMethod method,
        String bankTransactionCode,
        Long costStatementId,
        Long originalTransactionId,
        Long linkedTransactionId,
        String createdBy,
        String approvedBy,
        String reason,
        String note,
        TransactionStatus status,
        boolean adjusted,
        Long approvalRequestId) {

    public static final String SYSTEM = "SYSTEM";

    public static BalanceTransactionView of(BalanceTransactionRow t) {
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
                t.createdBy() == null ? SYSTEM : t.createdBy().toString(),
                t.approvedBy() == null ? null : t.approvedBy().toString(),
                t.reason(),
                t.note(),
                t.status(),
                t.adjusted(),
                t.approvalRequestId());
    }
}
