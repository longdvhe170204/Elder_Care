package com.eldercare.backend.medication.internal.status.domain;

/**
 * Sự kiện nội bộ của {@code medication}: lô thuốc gia đình gửi vừa hoàn trả, hủy hoặc lần giao thuốc mang theo vừa nhận
 * lại; nghe đồng bộ trong cùng transaction để 004 tính lại danh sách việc sau qua đời (004 FR-071, 006 research D-14).
 */
public record StoredMedicationClosed(long residentId) {}
