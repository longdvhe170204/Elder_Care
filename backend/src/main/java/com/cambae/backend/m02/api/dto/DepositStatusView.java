package com.cambae.backend.m02.api.dto;

import java.time.Instant;

/**
 * Trạng thái đặt cọc kèm căn cứ (giao dịch làm đổi gần nhất, Kế toán lập). {@code at}, {@code transactionId},
 * {@code accountantId} rỗng khi chưa có giao dịch. Provisional: chữ ký view chưa được 017 (E) chốt.
 */
public record DepositStatusView(DepositStatus status, Instant at, Long transactionId, Long accountantId) {}
