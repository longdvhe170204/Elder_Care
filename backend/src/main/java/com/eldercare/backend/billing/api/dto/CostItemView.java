package com.eldercare.backend.billing.api.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Kiểu đọc liên module của 010; không phải bản cài nghiệp vụ hoặc bộ tạo phản hồi HTTP. */
public record CostItemView(
        long id,
        CostType costType,
        String origin,
        BillingSourceView billingSource,
        String description,
        String itemCode,
        String medicationName,
        LocalDate incurredOn,
        BigDecimal quantity,
        String unit,
        Long unitPrice,
        Map<String, Object> unitPriceBasis,
        BigDecimal factor,
        Long amount,
        List<String> flags,
        String status,
        Long originalItemId,
        String originalPeriod,
        List<Map<String, Object>> recalculationHistory,
        List<Map<String, Object>> adjustments,
        Long currentTotal,
        List<String> hiddenFields) {}
