package com.eldercare.backend.billing.balance.internal.ledger.domain;

/**
 * Thông tin nộp tiền (schema {@code PaymentInstructions}; FR-003): ngân hàng (tên viết tắt trong danh mục), tài khoản thu
 * CFG-M11-10, mã nộp tiền, chuỗi VietQR không số tiền (client vẽ ảnh).
 */
public record PaymentInstructions(
        String bank, String accountNumber, String accountHolder, String paymentCode, String vietQr) {}
