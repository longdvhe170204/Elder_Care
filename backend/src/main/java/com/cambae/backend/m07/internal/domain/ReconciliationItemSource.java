package com.cambae.backend.m07.internal.domain;

/**
 * Information source for medication reconciliation item line (FR-036).
 */
public enum ReconciliationItemSource {
    /** Current prescription in the system */
    CURRENT_PRESCRIPTION,
    /** Discharge prescription brought from hospital */
    DISCHARGE_PRESCRIPTION,
    /** Medication in use upon initial admission */
    ADMISSION_CURRENT_MEDICATION
}
