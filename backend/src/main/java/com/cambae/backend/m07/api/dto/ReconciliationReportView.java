package com.cambae.backend.m07.api.dto;

import java.time.Instant;

/**
 * Reconciliation report item view.
 */
public record ReconciliationReportView(
        long reconciliationId, long residentId, String reason, String status, Instant deadline, boolean isOverdue) {}
