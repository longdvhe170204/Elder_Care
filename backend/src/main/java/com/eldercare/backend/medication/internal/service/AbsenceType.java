package com.eldercare.backend.medication.internal.service;

/**
 * Type of resident absence affecting medication administration (FR-021, FR-027, BR-M07-03).
 */
public enum AbsenceType {
    TEMPORARY_LEAVE, // TAM_VANG (no medication -> SUSPENDED)
    HOSPITALIZATION, // CHUYEN_VIEN (in hospital -> SUSPENDED)
    TRIP_WITH_MEDICATION // CHUYEN_DI (trip with medication handover -> LEAVE_MEDICATION)
}
