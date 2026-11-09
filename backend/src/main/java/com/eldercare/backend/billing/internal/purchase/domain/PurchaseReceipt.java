package com.eldercare.backend.billing.internal.purchase.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/** Bản ghi con chỉ thêm để chuỗi lưu giữ nhận đúng chủ sở hữu tệp chứng từ. */
public record PurchaseReceipt(long fileId) {
    public static final SharedKind<PurchaseReceipt> KIND =
            SharedKind.of("M11.PURCHASE_ASSISTANCE_RECEIPT", PurchaseReceipt.class);
}
