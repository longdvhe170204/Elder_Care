package com.eldercare.backend.common.notification;

/** Nhóm nguồn kích hoạt của mục 17 (009 FR-001); nhóm khác do module nguồn khai báo dùng OTHER. */
public enum SourceGroup {
    INCIDENT,
    HEALTH_ALERT,
    MEDICATION,
    OVERDUE_TASK,
    SCHEDULE_CHANGE,
    LICENSE_EXPIRING,
    COSTS,
    FAMILY_INFO,
    OTHER
}
