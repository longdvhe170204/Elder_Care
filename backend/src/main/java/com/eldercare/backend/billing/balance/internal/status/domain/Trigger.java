package com.eldercare.backend.billing.balance.internal.status.domain;

/** Lần tính tình trạng: hằng ngày của Bộ lập lịch, hoặc ngay khi ghi giao dịch nộp tiền (bảng chuyển tình trạng số dư). */
public enum Trigger {
    DAILY,
    PAYMENT
}
