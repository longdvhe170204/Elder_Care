package com.eldercare.backend.billing.internal.item.domain;

import java.time.*;
import java.util.Map;

public record StoredCostCalculation(
        long id,
        long residentId,
        long statementId,
        long periodId,
        String status,
        String sourceType,
        String sourceId,
        String billingUnit,
        Long serviceId,
        LocalDate date,
        Instant occurredAt,
        CostRecalculation.Calculation calculation,
        Map<String, Object> sourceValues) {}
