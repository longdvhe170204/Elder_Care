package com.eldercare.backend.medication.api.dto;

import java.time.Instant;

/**
 * Confirmed medication reconciliation view.
 */
public record ConfirmedReconciliationView(
        long reconciliationId, long residentId, String reason, Instant confirmedAt, Long confirmedByStaffId) {}
