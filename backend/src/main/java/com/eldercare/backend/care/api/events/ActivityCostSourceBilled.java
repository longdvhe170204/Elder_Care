package com.eldercare.backend.care.api.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Nguồn tính phí của hoạt động (014 FR-032, FR-040; 010 FR-005): một lượt có mặt ở buổi hoặc lượt rời viện của hoạt động
 * có tính phí. Cùng trường với {@code billing.api.events.CostSourceBilled}; khai ở {@code care} để care không phụ thuộc
 * billing (billing đã nghe care: tránh vòng module, ModuleBoundaryTest 2026-11-07). 010 nghe và ghi chỉ mục nguồn.
 */
public record ActivityCostSourceBilled(
        String sourceType,
        String sourceId,
        String unit,
        long residentId,
        Optional<Long> serviceId,
        BigDecimal quantity,
        LocalDate incurredOn,
        Instant occurredAt,
        String eventKey) {}
