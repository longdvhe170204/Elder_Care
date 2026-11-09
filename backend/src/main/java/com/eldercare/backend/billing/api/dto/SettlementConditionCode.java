package com.eldercare.backend.billing.api.dto;

/** Hai mã điều kiện kết thúc lưu trú dòng (h) do 017 trả (FR-022, BR-M11-22; tên logic {@code KT_H_*}). */
public enum SettlementConditionCode {
    /** Ba ý về tiền; miễn được bằng ngoại lệ đã duyệt của 004. */
    END_H_SETTLEMENT,
    /** Không còn giao dịch Chờ duyệt, đã xác nhận quyết toán và số liệu không đổi; không miễn được. */
    END_H_SETTLEMENT_CONFIRMATION
}
