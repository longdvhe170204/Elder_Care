package com.eldercare.backend.medication.internal.domain;

/**
 * Status of a dose review record (data-model section 5, FR-029).
 */
public enum DoseReviewStatus {
    PENDING_REVIEW, // CHO_XEM_LAI
    ACCEPTED, // CHAP_NHAN
    REQUIRES_CORRECTION // CAN_DINH_CHINH
}
