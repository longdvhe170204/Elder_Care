package com.cambae.backend.m07.api.dto;

/**
 * Status of a single medication dose.
 */
public enum DoseStatus {
    SCHEDULED, // Scheduled
    DUE, // Due
    LATE, // Late
    MISSED, // Missed
    ADMINISTERED, // Administered
    REFUSED, // Refused
    NOT_ADMINISTERED, // Not administered
    SUSPENDED, // Suspended
    DISPENSED_FOR_TRIP, // Carried for trip
    CANCELLED // Cancelled
}
