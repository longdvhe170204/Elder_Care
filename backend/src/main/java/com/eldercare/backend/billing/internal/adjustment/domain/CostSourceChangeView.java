package com.eldercare.backend.billing.internal.adjustment.domain;

import java.time.Instant;
import java.util.Map;

/** Giá trị nguồn trước/sau, không chứa số tiền điều chỉnh gợi ý. */
public record CostSourceChangeView(
        long id,
        long itemId,
        long residentId,
        String event,
        Map<String, Object> before,
        Map<String, Object> after,
        String reference,
        Instant occurredAt) {}
