package com.eldercare.backend.resident.api.events;

/** Dị ứng khác nhập tay được ghi nhận; 011 nghe (011 FR-036a). */
public record OtherAllergyRecorded(long itemId, long residentId) {}
