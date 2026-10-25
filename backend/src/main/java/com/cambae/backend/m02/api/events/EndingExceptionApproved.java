package com.cambae.backend.m02.api.events;

/**
 * Ngoại lệ điều kiện kết thúc đã duyệt; 017 tính lại điều kiện quyết toán (FR-064, FR-022, FR-034).
 * {@code residencyEndingId} rỗng khi ngoại lệ của mục quyết toán sau qua đời (Q-387).
 */
public record EndingExceptionApproved(Long residencyEndingId, long residentId, String conditionCode) {}
