package com.eldercare.backend.belongings.internal.belonging.domain;

/** Căn cứ người nhận đồ (FR-016, Q-253; data-model mục 5 {@code can_cu}). */
public enum ReturnBasis {
    /** Người đại diện hoặc người thân có quyền đón tại thời điểm trả. */
    ENTITLED_RECIPIENT,
    /** Chính người cao tuổi nhận đồ không thuộc loại cần đồng ý (Q-253). */
    RESIDENT_Q253_EXCEPTION,
    /** Xác nhận người nhận khác Hiệu lực (FR-017). */
    RECIPIENT_CONFIRMATION
}
