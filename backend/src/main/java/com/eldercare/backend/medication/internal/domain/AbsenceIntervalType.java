package com.eldercare.backend.medication.internal.domain;

/**
 * Resident absence interval type (FR-048, BR-M07-03).
 */
public enum AbsenceIntervalType {
    TEMPORARY_LEAVE_NO_MED,
    TEMPORARY_LEAVE_WITH_MED,
    HOSPITAL,
    TRIP_WITH_MED,
    TRIP_NO_MED
}
