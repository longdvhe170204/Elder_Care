package com.eldercare.backend.billing.internal.period.domain;

import java.time.*;

public record CostStatementRecord(
        long id,
        long periodId,
        long residentId,
        YearMonth month,
        String statementType,
        boolean finalPeriod,
        LocalDate throughDate,
        String status,
        Long total,
        int version) {}
