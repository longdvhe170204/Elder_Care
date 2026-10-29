package com.eldercare.backend.notification.api.dto;

/** Cách xác nhận thông báo Khẩn cấp (FR-026, FR-030; Q-104). */
public enum AcknowledgementMethod {
    IN_APP,
    BY_CALL,
    LATE,
    LATE_BY_CALL
}
