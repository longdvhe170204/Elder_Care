package com.cambae.backend.m07.internal.service;

import java.time.Instant;

/**
 * Representation of an absence window for a resident (FR-021, BR-M07-03).
 */
public record ResidentAbsenceInterval(Long residentId, Instant fromTime, Instant toTime, AbsenceType absenceType) {

    public boolean covers(Instant time) {
        if (time == null) {
            return false;
        }
        boolean afterFrom = fromTime == null || !time.isBefore(fromTime);
        boolean beforeTo = toTime == null || !time.isAfter(toTime);
        return afterFrom && beforeTo;
    }
}
