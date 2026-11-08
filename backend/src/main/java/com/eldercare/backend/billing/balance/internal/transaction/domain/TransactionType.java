package com.eldercare.backend.billing.balance.internal.transaction.domain;

/** Loại giao dịch số dư (017 FR-010; glossary 23.3). */
public enum TransactionType {
    CASH_PAYMENT,
    BANK_TRANSFER_PAYMENT,
    STATEMENT_PAYMENT,
    REFUND,
    DEPOSIT_OFFSET,
    ADJUSTMENT,
    DEPOSIT_RECEIPT,
    DEPOSIT_REFUND
}
