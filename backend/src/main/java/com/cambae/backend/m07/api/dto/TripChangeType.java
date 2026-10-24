package com.cambae.backend.m07.api.dto;

/**
 * Trip change type for resident outings / hospital transfers.
 */
public enum TripChangeType {
    CANCEL_TRIP, // Cancel trip
    NOT_ATTENDING, // Resident not attending
    RESCHEDULE // Reschedule trip dates
}
