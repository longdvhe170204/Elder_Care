package com.eldercare.backend.medication.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Supplied medication intake result by procurement order (FR-054).
 */
public record IntakeResultView(
        long intakeLineId,
        long procurementLineId,
        String medicationName,
        BigDecimal quantityReceived,
        boolean passed,
        String note,
        Instant receivedAt) {}
