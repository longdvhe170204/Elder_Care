package com.eldercare.backend.medication.internal.domain;

/**
 * Prescription lifecycle status (FR-011, data-model section 4).
 */
public enum PrescriptionStatus {
    /** Not yet reached effective start date (or pending admission) */
    PENDING_EFFECTIVE,
    /** Currently active */
    ACTIVE,
    /** Currently suspended */
    SUSPENDED,
    /** Discontinued */
    DISCONTINUED,
    /** Expired past end date */
    EXPIRED
}
