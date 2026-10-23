package com.cambae.backend.m07.api.dto;

import java.time.Instant;

/**
 * Phiếu đối chiếu thuốc đã xác nhận (PhieuDoiChieuView).
 */
public record ConfirmedReconciliationView(
        long reconciliationId, long residentId, String reason, Instant confirmedAt, Long confirmedByStaffId) {}
