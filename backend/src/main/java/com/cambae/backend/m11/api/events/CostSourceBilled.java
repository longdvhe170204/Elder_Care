package com.cambae.backend.m11.api.events;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;

/** FR-005: 011/012/014 phát từ lệnh nguồn; 010 chỉ khai báo hợp đồng, chưa có producer ở đây. */
public record CostSourceBilled(
        String sourceType,
        String sourceId,
        String unit,
        long residentId,
        Optional<Long> serviceId,
        BigDecimal quantity,
        LocalDate incurredOn,
        Instant occurredAt,
        String eventKey) {}
