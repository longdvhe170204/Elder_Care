package com.eldercare.backend.health.api.dto;

/** Vòng xác nhận bản ghi chỉ số (FR-015 đến FR-017). */
public enum VitalSignConfirmationStatus {
    AWAITING_REMEASUREMENT,
    CONFIRMED,
    REMEASURED
}
