package com.eldercare.backend.billing.balance.internal.status.domain;

import java.time.Instant;

/** Hồ sơ cần nhắc hoàn cọc (017 FR-015a): số dư cọc, lần nhắc trước (rỗng khi chưa nhắc). */
public record DepositRefundDue(long residentId, long depositBalance, Instant remindedAt) {}
