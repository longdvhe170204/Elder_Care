package com.eldercare.backend.billing.api.dto;

/**
 * Sổ số dư (tên logic {@code SoDuView}): tiền nguyên đồng, có dấu (số dư âm là còn nợ). {@code daysCovered} rỗng khi
 * không tính được (phí một ngày bằng 0 hoặc chưa có giá).
 */
public record BalanceView(
        long residentId,
        String paymentCode,
        long balance,
        long depositBalance,
        BalanceStatus status,
        Integer daysCovered) {}
