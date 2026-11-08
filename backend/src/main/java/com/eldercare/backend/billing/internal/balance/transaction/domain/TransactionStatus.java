package com.eldercare.backend.billing.internal.balance.transaction.domain;

/** Trạng thái giao dịch số dư (glossary 23.3); chỉ {@link #CONFIRMED} làm đổi số dư. */
public enum TransactionStatus {
    PENDING_APPROVAL,
    CONFIRMED,
    REJECTED,
    CANCELLED
}
