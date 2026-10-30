package com.cambae.backend.m07.internal.domain;

/**
 * Medication reconciliation process workflow status (FR-035).
 */
public enum ReconciliationStatus {
    /** Open, allows editing and adding reconciliation lines */
    OPEN,
    /** Submitted, pending review and confirmation by a second staff member */
    PENDING_CONFIRMATION,
    /** Confirmed, decisions applied to prescriptions */
    CONFIRMED,
    /** Cancelled (re-hospitalization, admission cancellation, or final discharge state) */
    CANCELLED
}
