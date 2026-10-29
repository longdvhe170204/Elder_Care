package com.eldercare.backend.resident.api.events;

/** Thuốc đang sử dụng khi tiếp nhận đổi; 006 nghe (006 FR-034a, BR-M07-22; FR-017). */
public record InitialHealthRecordChanged(long residentId) {}
