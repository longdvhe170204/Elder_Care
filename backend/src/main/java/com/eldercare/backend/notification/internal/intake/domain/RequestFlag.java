package com.eldercare.backend.notification.internal.intake.domain;

/** Dấu đặt lên yêu cầu lúc tiếp nhận ({@code dau}; FR-001a, FR-003, FR-011, FR-013, FR-015). */
public enum RequestFlag {
    MISSING_LEVEL,
    MISSING_INFORMATION_TYPE,
    MISSING_SHORT_CONTENT,
    NO_RECIPIENT,
    AFTER_DEATH,
    MISSING_INFORMATION_SUBJECT,
    MISSING_Q354_CASE,
    /** Nội dung rút gọn có trường sức khỏe hoặc thiếu: phần kênh ngoài ứng dụng không gửi (FR-020). */
    INVALID_OUT_OF_APP_CONTENT
}
