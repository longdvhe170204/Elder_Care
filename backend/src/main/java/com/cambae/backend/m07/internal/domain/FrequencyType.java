package com.cambae.backend.m07.internal.domain;

/**
 * Frequency type of scheduled prescription (FR-004).
 */
public enum FrequencyType {
    /** Fixed time slots during the day (e.g. 08:00, 20:00) */
    TIMED_SLOTS,
    /** Every n consecutive hours starting from an initial time (e.g. every 6 hours from 06:00) */
    EVERY_N_HOURS,
    /** Specific days of the week */
    DAYS_OF_WEEK
}
