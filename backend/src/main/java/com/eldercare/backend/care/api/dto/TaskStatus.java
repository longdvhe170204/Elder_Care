package com.eldercare.backend.care.api.dto;
/** T003: tên tương đương trạng thái/mã trong hợp đồng 005. */
public enum TaskStatus {
    /** Q-453: công việc lập theo tháng, chưa công bố; nhân viên không thấy, không nhắc, không quá hạn. */
    DRAFT,
    NOT_YET_DUE,
    DUE,
    OVERDUE,
    COMPLETED,
    COMPLETED_LATE,
    NOT_PERFORMED,
    CANCELLED
}
