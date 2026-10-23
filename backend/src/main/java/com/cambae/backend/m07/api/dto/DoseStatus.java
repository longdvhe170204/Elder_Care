package com.cambae.backend.m07.api.dto;

/**
 * Trạng thái của từng liều thuốc (TrangThaiLieu).
 */
public enum DoseStatus {
    SCHEDULED, // Chưa đến giờ
    DUE, // Đến giờ
    LATE, // Trễ
    MISSED, // Bỏ lỡ
    ADMINISTERED, // Đã dùng
    REFUSED, // Từ chối
    NOT_ADMINISTERED, // Không thực hiện
    SUSPENDED, // Tạm dừng
    DISPENSED_FOR_TRIP, // Mang theo khi ra ngoài
    CANCELLED // Đã hủy
}
