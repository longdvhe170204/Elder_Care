package com.eldercare.backend.incident.api.events;

/** Đã có kết quả xác nhận lại nguyện vọng; 009 chỉ dừng lượt của nguồn này (FR-050b d). */
public record WishReconfirmationCompleted(long alertId) {}
