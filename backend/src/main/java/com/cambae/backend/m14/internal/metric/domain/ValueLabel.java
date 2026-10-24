package com.cambae.backend.m14.internal.metric.domain;

/** Nhãn của một giá trị chỉ tiêu (FR-007, FR-008; contracts {@code MetricValue.labels}). */
public enum ValueLabel {
    /** Mẫu số 0: hiện "—" kèm "không có dữ liệu". */
    NO_DATA,
    /** Dữ liệu cấu hình chỉ tiêu cần chưa có. */
    NOT_CONFIGURED,
    /** Khoảng chứa hôm nay. */
    DAY_NOT_ENDED,
    /** Có bản ghi ngoại tuyến chờ xem lại; số ở {@code pendingReviewCount}. */
    PENDING_REVIEW,
    /** Khoảng giao với dữ liệu đã loại bỏ theo NFR-07. */
    PURGED_BY_RETENTION,
    /** Chi phí ở chế độ tạm tính (FR-051). */
    PROVISIONAL,
    /** Số đếm chỉ trong phạm vi của người xem, với vai trò có phạm vi "P" (FR-020 khoanh vùng). */
    WITHIN_YOUR_SCOPE
}
