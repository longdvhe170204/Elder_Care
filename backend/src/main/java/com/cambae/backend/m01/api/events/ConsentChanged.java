package com.cambae.backend.m01.api.events;

/**
 * Bản đồng ý Hiệu lực đổi; 009, 012 nghe (FR-015). {@code activeConsentId} là bản Hiệu lực sau khi đổi, rỗng khi không
 * còn bản nào (rút lại mà chưa ghi bản mới).
 */
public record ConsentChanged(long residentId, Long activeConsentId) {}
