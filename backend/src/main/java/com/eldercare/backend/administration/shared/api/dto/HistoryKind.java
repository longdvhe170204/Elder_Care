package com.eldercare.backend.administration.shared.api.dto;

/** Kiểu dòng của {@code status_history}: một mốc, hoặc một khoảng "từ … đến …" ({@code ends_at} đặt một lần). */
public enum HistoryKind {
    POINT,
    PERIOD
}
