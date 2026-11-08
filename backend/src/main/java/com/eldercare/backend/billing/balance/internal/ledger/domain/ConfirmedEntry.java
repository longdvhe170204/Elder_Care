package com.eldercare.backend.billing.balance.internal.ledger.domain;

import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import java.time.Instant;

/**
 * Giao dịch Đã xác nhận ngay khi ghi (FR-005, FR-007, FR-012): nộp tiền mặt, chuyển khoản, thu cọc, thanh toán bảng.
 * {@code at} là thời điểm thu, ngày nhận, hoặc lúc chốt bảng.
 */
public record ConfirmedEntry(
        long residentId,
        Ledger ledger,
        TransactionType type,
        long amount,
        Instant at,
        PaymentMethod method,
        String bankTransactionCode,
        Long costStatementId,
        String payerName,
        String note,
        boolean duplicateCodeConfirmed,
        Long createdBy) {}
