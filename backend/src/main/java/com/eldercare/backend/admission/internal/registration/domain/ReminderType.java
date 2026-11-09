package com.eldercare.backend.admission.internal.registration.domain;

/**
 * Loại nhắc của khai báo, khóa chống nhắc trùng {@code registration_reminder} (data-model mục 4; research Z-06): nhắc lập,
 * quá hạn, gia hạn, khai báo xóa.
 */
public enum ReminderType {
    CREATE,
    OVERDUE,
    OVERDUE_MANAGER,
    RENEWAL,
    DEREGISTER
}
