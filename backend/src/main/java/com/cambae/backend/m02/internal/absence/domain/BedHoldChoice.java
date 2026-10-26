package com.cambae.backend.m02.internal.absence.domain;

/** Lựa chọn của Quản lý viện ở lệnh Quyết định giữ giường (FR-055, FR-061; glossary 10.3). */
public enum BedHoldChoice {
    /** Giữ tiếp: bắt buộc hệ số phí và ngày xem xét lại. */
    HOLD_CONTINUE,
    /** Giải phóng: hệ số phí cho các ngày còn lại của lượt vắng, giường giải phóng ở 003. */
    RELEASE,
    /** Lượt đã đóng: chỉ đặt hệ số cho các ngày bị ảnh hưởng, không còn giường để giữ (BR-M02-18). */
    COEFFICIENT_ONLY
}
