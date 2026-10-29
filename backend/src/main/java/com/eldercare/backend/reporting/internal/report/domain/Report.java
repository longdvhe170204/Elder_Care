package com.eldercare.backend.reporting.internal.report.domain;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Một báo cáo đã tính ({@code BaoCao}; contracts {@code Report}): {@code asOf} là "tính đến" lúc lấy số; {@code scope} là
 * hợp bộ lọc thô của phạm vi các nhóm chỉ tiêu, ghi vào nhật ký lần xuất (FR-062).
 */
public record Report(
        ReportGroup group,
        LocalDate from,
        LocalDate to,
        Instant asOf,
        ReportFilters filters,
        ScopeFilter scope,
        List<MetricResult> results) {}
