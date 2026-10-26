package com.cambae.backend.m02.internal.absence.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Quyết định giữ giường (nhóm 3, data-model mục 8). {@code appliesFrom}, {@code appliesTo} là các ngày vắng mà hệ số của
 * quyết định thay hệ số của dòng chính sách; {@code appliesTo} rỗng là tới hết lượt vắng hoặc tới quyết định sau.
 */
public record BedHoldDecision(
        long id,
        long absenceId,
        LocalDate thresholdDate,
        BedHoldChoice choice,
        BigDecimal coefficient,
        LocalDate reviewDate,
        LocalDate appliesFrom,
        LocalDate appliesTo,
        String reason,
        long decidedBy,
        Instant decidedAt) {

    public boolean covers(LocalDate day) {
        return !day.isBefore(appliesFrom) && (appliesTo == null || !day.isAfter(appliesTo));
    }
}
