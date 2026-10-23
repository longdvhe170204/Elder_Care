package com.cambae.backend.m07.api.dto;

import java.time.Instant;

/**
 * Dòng thống kê phiếu đối chiếu thuốc (PhieuDoiChieuBaoCaoView).
 */
public record ReconciliationReportView(
        long reconciliationId, long residentId, String reason, String status, Instant deadline, boolean isOverdue) {}
