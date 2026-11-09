package com.eldercare.backend.billing.balance.internal.ledger.domain;

/**
 * Loại đối tượng của phần số dư 017 (glossary mục 23.5; data-model mục 8): mã nhật ký, phạm vi, lưu giữ. Mã của sổ là
 * mã người cao tuổi (một sổ mỗi hồ sơ, FR-001).
 */
public final class BalanceTypes {

    /** Sổ số dư (tên logic {@code M11.SO_DU}). */
    public static final String LEDGER = "M11.BALANCE";

    /** Giao dịch số dư (tên logic {@code M11.GIAO_DICH_SO_DU}); cũng là loại yêu cầu phê duyệt. */
    public static final String TRANSACTION = "M11.BALANCE_TRANSACTION";

    private BalanceTypes() {}
}
