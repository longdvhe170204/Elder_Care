package com.eldercare.backend.billing.balance.internal.status.domain;

/**
 * Thông báo của một lần tính (FR-018 → FR-019, FR-030): tình trạng (người đại diện mức Trung bình, Kế toán mức Nhẹ), Quản
 * lý viện khi còn nợ kéo dài, nộp tiền đã đủ (Q-252), nộp tiền đã ghi nhận nhưng chưa đủ (Q-257).
 */
public enum Notice {
    STATUS,
    MANAGER,
    PAYMENT_SUFFICIENT,
    PAYMENT_RECORDED
}
