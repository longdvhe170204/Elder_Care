package com.eldercare.backend.reporting.internal.metric.domain;

/** Phạm vi của một ô bảng FR-010 (research O-03: {@code TOAN_VIEN}, {@code TANG_DUOC_GIAO}, {@code PHAN_CONG}…). */
public enum GroupScope {
    /** Toàn viện. */
    FACILITY,
    /** Tầng/khu đang được giao lúc xem, mọi thời điểm (FR-012). */
    ASSIGNED_FLOORS,
    /** Phạm vi phân công: đối tượng phân công trong giờ ca ± CFG-M15-07 (FR-012). */
    ASSIGNMENT,
    /** Tầng/khu của phân công (khoanh vùng, bàn giao; chỉ dashboard). */
    ASSIGNMENT_AREA,
    /** Nhân viên có phân công tại tầng được giao. */
    FLOOR_STAFF
}
