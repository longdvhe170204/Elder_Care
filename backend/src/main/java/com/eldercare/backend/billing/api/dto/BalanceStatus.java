package com.eldercare.backend.billing.api.dto;

/** Tình trạng số dư (FR-016 → FR-019; tên logic {@code BINH_THUONG}, {@code SAP_HET_TIEN}, {@code CON_NO}). */
public enum BalanceStatus {
    NORMAL,
    RUNNING_LOW,
    IN_DEBT
}
