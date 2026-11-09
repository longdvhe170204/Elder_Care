package com.eldercare.backend.billing.internal.source.domain;

import com.eldercare.backend.billing.api.dto.BillingSourceView;
import com.eldercare.backend.billing.api.dto.CostType;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Nguồn tại lần đọc; facts giữ dữ liệu nguồn để truy vết, không phải đơn giá đề xuất. */
public record CostSourceCandidate(
        long residentId,
        String sourceType,
        String sourceId,
        String billingUnit,
        LocalDate date,
        Instant occurredAt,
        Long serviceId,
        CostType costType,
        String unit,
        String description,
        BigDecimal quantity,
        BigDecimal factor,
        Set<String> flags,
        Long existingItemId,
        String blockedCode,
        String blockedReason,
        Map<String, Object> facts) {
    public BillingSourceView view() {
        return new BillingSourceView(
                sourceType,
                sourceId,
                unit,
                date,
                occurredAt,
                description,
                quantity,
                factor,
                List.copyOf(flags),
                existingItemId,
                billingUnit);
    }
}
