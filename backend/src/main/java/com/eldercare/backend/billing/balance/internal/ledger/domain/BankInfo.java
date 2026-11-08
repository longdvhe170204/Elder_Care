package com.eldercare.backend.billing.balance.internal.ledger.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Thuộc tính của một ngân hàng trong danh mục {@code M11.BANK} (tên logic {@code ngan_hang}; research E-04, R-15): mã
 * của mục là BIN NAPAS, tên là tên viết tắt. Nạp, tắt chỉ bằng migration ({@code V017_02}); không có lệnh sửa.
 */
public record BankInfo(String fullName) {

    public static final SharedKind<BankInfo> KIND =
            SharedKind.of("M11.BANK", BankInfo.class).notDeletable();
}
