package com.eldercare.backend.medication.api.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event fired when medication fee is incurred for facility-provided medication (FR-025a, FR-033, FR-048a).
 * Module M11 (010) listens to record facility charges. {@code usageUnit} là đơn vị dùng của đơn nguồn (viên, ml…), không
 * kèm tên thuốc; {@code medicationId} là mã vật phẩm (bàn giao cho 010 T011, 2026-11-08). Lượng giữ nguyên độ chính xác
 * (DECIMAL 12,3 với lần dùng PRN và lần giao).
 */
public record MedicationBilledEvent(
        BilledSource source,
        long sourceId,
        long residentId,
        long medicationId,
        BigDecimal quantity,
        String usageUnit,
        Instant occurredAt) {}
