package com.eldercare.backend.billing.api.events;

import com.eldercare.backend.billing.api.dto.BalanceStatus;

/** Tình trạng số dư đổi (tên logic {@code TinhTrangSoDuDaDoi}; FR-017); 016 nghe. */
public record BalanceStatusChanged(long residentId, BalanceStatus status) {}
