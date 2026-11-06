package com.eldercare.backend.billing.internal.period.domain;

import java.time.LocalDate;

/** Ảnh đọc dưới khóa của bảng thường; không phải DTO HTTP. */
public record DraftCostStatement(
        long id, long periodId, long residentId, String status,
        boolean finalPeriod, LocalDate throughDate, int version) {}
