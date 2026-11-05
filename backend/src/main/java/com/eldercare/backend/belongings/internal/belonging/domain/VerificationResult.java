package com.eldercare.backend.belongings.internal.belonging.domain;

/** Kết quả xác minh giấy tờ người nhận (FR-016; data-model mục 5 {@code ket_qua_xac_minh}). */
public enum VerificationResult {
    /** Giấy tờ xuất trình khớp loại và số đã đăng ký ở hồ sơ người thân. */
    MATCHED,
    /** Hồ sơ người thân chưa có giấy tờ: ghi giấy tờ xuất trình, đối chiếu họ tên, nhắc Hành chính bổ sung. */
    NO_DOCUMENT_ON_FILE_CROSS_CHECKED
}
