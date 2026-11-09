package com.eldercare.backend.billing.balance.internal.ledger.domain;

import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import java.time.Instant;

/**
 * Một dòng {@code balance_transaction} (data-model 017 mục 3). {@code createdBy} là tài khoản lập, rỗng với giao dịch
 * hệ thống tạo kèm lệnh chốt bảng (FR-012).
 */
public record BalanceTransactionRow(
        long id,
        long residentId,
        Ledger ledger,
        TransactionType type,
        long amount,
        Instant at,
        PaymentMethod method,
        String bankTransactionCode,
        Long costStatementId,
        Long originalTransactionId,
        Long linkedTransactionId,
        String payerName,
        String note,
        String reason,
        TransactionStatus status,
        Long approvalRequestId,
        Long approvedBy,
        Instant confirmedAt,
        boolean adjusted,
        Long createdBy) {}
