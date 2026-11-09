package com.eldercare.backend.medication.internal.dose.service;

import java.math.BigDecimal;
import java.time.Instant;

/** Nội dung lần dùng PRN ghi ngoại tuyến ({@code M07.PRN_ADMINISTRATION}; T045, research D-11). */
public record PrnOfflineContent(
        Long prescriptionId,
        Long residentId,
        BigDecimal dosage,
        String reason,
        String reportedBy,
        Instant administeredAt,
        String adverseReaction,
        String note) {}
