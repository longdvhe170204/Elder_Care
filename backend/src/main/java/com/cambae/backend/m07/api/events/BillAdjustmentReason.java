package com.cambae.backend.m07.api.events;

/**
 * Lý do điều chỉnh tính phí thuốc (DINH_CHINH | NHAN_LAI).
 */
public enum BillAdjustmentReason {
    CORRECTION, // Đính chính bản ghi
    RETURNED // Nhận lại thuốc mang theo chưa dùng
}
