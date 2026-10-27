package com.cambae.backend.m02.internal.registration.domain;

/**
 * Trạng thái khai báo (FR-005; glossary 24.3): Cần khai báo, Đã nộp, Đã xác nhận, Hết hạn, Đã xóa, Được thay thế, Đã hủy.
 * Bảng chuyển giữ ở trigger {@code trg_residence_registration_transition}.
 */
public enum RegistrationStatus {
    TO_SUBMIT,
    SUBMITTED,
    CONFIRMED,
    EXPIRED,
    DEREGISTERED,
    SUPERSEDED,
    CANCELLED
}
