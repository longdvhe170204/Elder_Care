package com.cambae.backend.m11.api.dto;

import java.time.*;
import java.util.*;

/** Kiểu đọc liên module của 010; không phải bản cài nghiệp vụ hoặc bộ tạo phản hồi HTTP. */
public record CostStatementReportView(
        long id,
        long residentId,
        long periodId,
        CostStatementType statementType,
        boolean finalPeriod,
        String status,
        LocalDate finalizationDueOn) {}
