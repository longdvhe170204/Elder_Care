package com.cambae.backend.m07.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Kết quả tiếp nhận thuốc cung ứng theo đợt mua sắm (KetQuaTiepNhanView, FR-054).
 */
public record IntakeResultView(
        long intakeLineId,
        long procurementLineId,
        String medicationName,
        BigDecimal quantityReceived,
        boolean passed,
        String note,
        Instant receivedAt) {}
