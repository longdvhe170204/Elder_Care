package com.eldercare.backend.billing.internal.item.domain;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/** Ảnh khoản đã khóa, dùng tính lại; không đưa metadata này ra API ngoài kiểm quyền. */
public record RepriceableCost(
        long id,
        long residentId,
        long statementId,
        long periodId,
        long serviceId,
        String sourceType,
        LocalDate incurredOn,
        String status,
        String statementStatus,
        int version,
        CostRecalculation.Calculation calculation,
        Set<String> flags,
        Map<String, Object> unitPriceBasis,
        Map<String, Object> sourceValues) {}
