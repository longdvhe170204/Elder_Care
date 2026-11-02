package com.eldercare.backend.billing.api.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Kiểu đọc liên module của 010; không phải bản cài nghiệp vụ hoặc bộ tạo phản hồi HTTP. */
public record CostItemReportView(
        long id,
        long residentId,
        CostType costType,
        String origin,
        long createdBy,
        Long serviceId,
        String itemCode,
        LocalDate incurredOn,
        BigDecimal quantity,
        Long unitPrice,
        Long amount,
        Set<String> flags,
        String status,
        Long originalItemId,
        String originalPeriod) {}
