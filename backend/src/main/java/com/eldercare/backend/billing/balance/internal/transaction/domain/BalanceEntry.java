package com.eldercare.backend.billing.balance.internal.transaction.domain;

import java.time.Instant;

/**
 * Một dòng giao dịch làm đầu vào cho sao kê, báo cáo thu chi (017 FR-025). {@code amount} có dấu (nộp dương, trừ bảng và
 * hoàn âm), đơn vị đồng; {@code reference} là mã giao dịch ngân hàng hoặc mã bảng chi phí để in ở cột tham chiếu.
 */
public record BalanceEntry(
        long residentId,
        Ledger ledger,
        TransactionType type,
        long amount,
        Instant at,
        TransactionStatus status,
        PaymentMethod method,
        String reference) {}
