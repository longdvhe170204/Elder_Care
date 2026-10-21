package com.cambae.backend.m01.api.events;

/** Bản đồng ý Hiệu lực đổi; 009, 012 nghe (FR-015). */
public record ConsentChanged(long residentId, long activeConsentId) {}
