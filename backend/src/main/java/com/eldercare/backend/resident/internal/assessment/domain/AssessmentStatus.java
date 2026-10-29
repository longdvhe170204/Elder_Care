package com.eldercare.backend.resident.internal.assessment.domain;

/** Nháp sửa được; Đã xác nhận và Đã hủy không sửa (FR-026, FR-032). */
public enum AssessmentStatus {
    DRAFT,
    CONFIRMED,
    DISCARDED;
}
