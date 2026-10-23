package com.cambae.backend.m07.api.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Sự kiện phát sinh tính phí thuốc nguồn viện (SuKienTinhPhiThuoc, FR-025a, FR-033, FR-048a).
 * Module M11 (010) lắng nghe để ghi nhận chi phí viện phí.
 */
public record MedicationBilledEvent(
        BilledSource source,
        long sourceId,
        long residentId,
        long medicationId,
        BigDecimal quantity,
        Instant occurredAt) {}
