package com.eldercare.backend.billing.balance.internal.status.domain;

import java.time.Instant;

/**
 * Sổ có giao dịch Đã xác nhận chưa nằm trong bảng quyết toán hiện hành (017 FR-024). {@code statementExists}: đã từng xác
 * nhận quyết toán. Tiền theo đồng.
 */
public record AfterFinalCandidate(
        long residentId,
        long balance,
        long depositBalance,
        Instant remindedAt,
        Instant lastConfirmedAt,
        boolean statementExists) {}
