package com.eldercare.backend.belongings.internal.belonging.domain;

/** Loại bản ghi bàn giao đồ gửi (data-model mục 4; research K-04). */
public enum HandoverType {
    INTAKE,
    GIVE_FOR_USE,
    TAKE_BACK,
    TRANSFER_CUSTODY,
    RETURN,
    REPORT_LOST,
    RECORD_DAMAGE,
    FOUND,
    DISPOSAL,
    /** Lần chặn trả (FR-016, SC-004): ghi lại nhưng không đổi đồ gửi; không phải lệnh bàn giao. */
    RETURN_BLOCKED
}
