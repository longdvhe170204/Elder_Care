package com.eldercare.backend.medication.internal.domain;

/**
 * Prescription type (FR-004, data-model section 4).
 */
public enum PrescriptionType {
    /** Periodic prescription with schedule frequency and time slots */
    PERIODIC,
    /** As-needed (PRN) prescription with minimum interval and max daily doses */
    PRN
}
