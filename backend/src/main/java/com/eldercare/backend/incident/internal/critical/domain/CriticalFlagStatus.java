package com.eldercare.backend.incident.internal.critical.domain;

/** Trạng thái dấu nguy kịch: Tạm, Đang mở, Đã gỡ (bảng trạng thái FR-050a; Đã gỡ là trạng thái cuối). */
public enum CriticalFlagStatus {
    PROVISIONAL,
    OPEN,
    REMOVED
}
