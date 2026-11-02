package com.eldercare.backend.billing.api.dto;

import java.time.*;
import java.util.*;

/** Kiểu đọc liên module của 010; không phải bản cài nghiệp vụ hoặc bộ tạo phản hồi HTTP. */
public record CostStatementView(
        long id,
        String period,
        long residentId,
        CostStatementType statementType,
        boolean finalPeriod,
        LocalDate throughDate,
        String status,
        List<CostTypeTotal> totalsByType,
        Long total,
        List<CostItemView> items) {}
