package com.eldercare.backend.resident.api.dto;

/** Hồ sơ sức khỏe ban đầu; 006 dùng {@code currentMedications} (thuốc đang dùng khi tiếp nhận). */
public record InitialHealthRecordView(long id, long residentId, String currentMedications, boolean confirmed) {}
