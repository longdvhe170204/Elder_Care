package com.eldercare.backend.billing.balance.web.response;

import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.billing.balance.internal.transaction.service.BalanceTransactionService;
import java.time.Instant;

/**
 * Phần tử của {@code GET /balance-transactions/pending-approval} (schema {@code PendingBalanceTransaction} = {@code
 * BalanceTransaction} + người cao tuổi; chủ dự án duyệt 2026-11-08, glossary 23.8).
 */
public record PendingBalanceTransactionResponse(
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
        Long approvalRequestId,
        long residentId,
        String residentName) {

    public static PendingBalanceTransactionResponse of(BalanceTransactionService.Pending p) {
        var t = p.transaction();
        return new PendingBalanceTransactionResponse(
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
                t.note(),
                t.status(),
                t.adjusted(),
                t.approvalRequestId(),
                p.residentId(),
                p.residentName());
    }
}
