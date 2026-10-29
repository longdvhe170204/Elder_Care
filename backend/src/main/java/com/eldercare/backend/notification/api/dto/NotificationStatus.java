package com.eldercare.backend.notification.api.dto;

/** Trạng thái thông báo theo bảng FR-044 (data-model mục 4). */
public enum NotificationStatus {
    HELD_FOR_QUIET_HOURS,
    NOT_SENT,
    SENT,
    DELIVERY_FAILED,
    VIEWED,
    ACKNOWLEDGED,
    UNREACHABLE
}
