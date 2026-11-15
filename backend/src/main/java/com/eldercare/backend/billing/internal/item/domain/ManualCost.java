package com.eldercare.backend.billing.internal.item.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Khoản nhập tay (FR-021) đã kiểm, kèm bảng đích. */
public record ManualCost(
        long residentId,
        long statementId,
        long periodId,
        String description,
        LocalDate incurredOn,
        BigDecimal quantity,
        String unit,
        long unitPrice,
        BigDecimal amount,
        String reason,
        List<Long> fileIds) {}
