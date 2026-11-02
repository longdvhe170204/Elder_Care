package com.eldercare.backend.billing.api.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Kiểu đọc liên module của 010; không phải bản cài nghiệp vụ hoặc bộ tạo phản hồi HTTP. */
public record BillingSourceView(
        String sourceType,
        String sourceId,
        String unit,
        LocalDate incurredOn,
        Instant occurredAt,
        String description,
        BigDecimal quantity,
        BigDecimal factor,
        List<String> flags,
        Long existingItemId) {}
