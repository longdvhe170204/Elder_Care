package com.eldercare.backend.billing.api.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Kiểu đọc liên module của 010; không phải bản cài nghiệp vụ hoặc bộ tạo phản hồi HTTP. */
public record PurchaseAssistanceView(
        long id,
        long residentId,
        Long catalogItemId,
        String description,
        BigDecimal quantity,
        long estimatedAmount,
        String reason,
        PurchaseRequesterView requester,
        PurchaseAssistanceStatus status,
        Long allowedAmount,
        boolean additionalConsentPending,
        boolean insufficientBalance,
        List<Map<String, Object>> decisions) {}
