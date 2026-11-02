package com.eldercare.backend.medication.internal.domain;

/**
 * Status of a family medication batch (FR-041, data-model section 8).
 */
public enum FamilyMedicationBatchStatus {
    /** Pending reconciliation against prescriptions */
    PENDING_RECONCILIATION,
    /** Currently in use (linked to active prescription) */
    IN_USE,
    /** Held only (not in active use or suspended) */
    HOLD_ONLY,
    /** Depleted (remaining quantity reached 0) */
    DEPLETED,
    /** Returned to resident's family */
    RETURNED,
    /** Disposed per family request */
    DISPOSED
}
