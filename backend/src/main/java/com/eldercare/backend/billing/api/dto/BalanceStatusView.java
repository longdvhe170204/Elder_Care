package com.eldercare.backend.billing.api.dto;

/** Hồ sơ Sắp hết tiền hoặc Còn nợ cho 016 (tên logic {@code SoDuTinhTrangView}). */
public record BalanceStatusView(long residentId, BalanceStatus status, long balance, Integer daysCovered) {}
