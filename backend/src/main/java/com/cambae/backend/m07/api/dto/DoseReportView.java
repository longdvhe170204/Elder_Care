package com.cambae.backend.m07.api.dto;

import java.time.Instant;

/**
 * Medication dose reporting statistics view (FR-051).
 */
public record DoseReportView(
        String doseKey,
        long residentId,
        long floorAreaId,
        Instant scheduledTime,
        DoseStatus finalStatus,
        Long closedByStaffId,
        String closingRule,
        boolean floorSharedDose,
        boolean dispensedForTrip,
        boolean administeredLate,
        boolean recordedOffline,
        String reviewResult) {}
