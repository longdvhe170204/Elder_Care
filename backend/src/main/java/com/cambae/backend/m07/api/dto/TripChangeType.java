package com.cambae.backend.m07.api.dto;

/**
 * Loại thay đổi chuyến đi ra ngoài / chuyển viện (LoaiThayDoiChuyen).
 */
public enum TripChangeType {
    CANCEL_TRIP, // Hủy chuyến
    NOT_ATTENDING, // Không đi
    RESCHEDULE // Đổi ngày
}
