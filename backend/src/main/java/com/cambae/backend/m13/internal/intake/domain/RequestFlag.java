package com.cambae.backend.m13.internal.intake.domain;

/** Dấu đặt lên yêu cầu lúc tiếp nhận ({@code dau}; FR-001a, FR-003, FR-011, FR-013, FR-015). */
public enum RequestFlag {
    MISSING_LEVEL,
    MISSING_INFORMATION_TYPE,
    MISSING_SHORT_CONTENT,
    NO_RECIPIENT,
    AFTER_DEATH,
    MISSING_INFORMATION_SUBJECT,
    MISSING_Q354_CASE
}
