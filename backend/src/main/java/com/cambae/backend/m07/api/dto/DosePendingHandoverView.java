package com.cambae.backend.m07.api.dto;

import java.time.Instant;

/**
 * Dose pending shift handover view (FR-028, Q-307).
 */
public record DosePendingHandoverView(
        String doseKey,
        long residentId,
        long prescriptionId,
        Instant scheduledTime,
        DoseStatus status,
        String reason,
        boolean controlledSubstance) {}
