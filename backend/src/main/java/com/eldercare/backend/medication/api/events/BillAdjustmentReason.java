package com.eldercare.backend.medication.api.events;

/**
 * Medication billing adjustment reason (CORRECTION | RETURNED).
 */
public enum BillAdjustmentReason {
    CORRECTION, // Record correction
    RETURNED // Returned unused carried medication
}
