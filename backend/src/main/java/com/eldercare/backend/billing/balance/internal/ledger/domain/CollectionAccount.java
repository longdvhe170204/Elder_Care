package com.eldercare.backend.billing.balance.internal.ledger.domain;

/**
 * Giá trị của CFG-M11-10 (kiểu bảng, research E-04; tên logic {@code maBin}, {@code soTaiKhoan}, {@code chuTaiKhoan}):
 * tài khoản thu của viện. Đọc bằng {@code ParameterApi.read(CFG_M11_10).asTable(CollectionAccount.class)}.
 */
public record CollectionAccount(String bin, String accountNumber, String accountHolder) {}
