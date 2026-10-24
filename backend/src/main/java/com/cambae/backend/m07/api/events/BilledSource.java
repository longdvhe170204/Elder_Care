package com.cambae.backend.m07.api.events;

/**
 * Source of medication charge event (DOSE | PRN_ADMINISTRATION | DISPENSE_RECORD).
 */
public enum BilledSource {
    DOSE, // Scheduled dose
    PRN_ADMINISTRATION, // PRN administration
    DISPENSE_RECORD // Medication handover record
}
