package com.eldercare.backend.admission.internal.ending.domain;

/** Trạng thái hồ sơ kết thúc lưu trú (FR-067): Đang chuẩn bị → Hoàn tất / Đã hủy. */
public enum EndingStatus {
    PREPARING,
    COMPLETED,
    CANCELLED;
}
