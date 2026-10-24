package com.cambae.backend.m14.web.response;

import com.cambae.backend.m14.internal.report.domain.MetricResult;
import com.cambae.backend.m14.internal.report.domain.Report;
import com.cambae.backend.m14.internal.report.domain.ReportGroup;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** {@code Report} của contracts/api-016.yaml; dòng chi tiết không có ở đây mà qua {@code /metric-details} (FR-009). */
public record ReportBody(
        String group,
        LocalDate from,
        LocalDate to,
        Instant asOf,
        Map<String, Object> filters,
        List<MetricValueBody> values,
        List<String> notes) {

    /** FR-050: chi phí của 016 là chi phí phát sinh của người cao tuổi, không phải báo cáo tài chính của viện. */
    static final String COSTS_NOTE = "Chi phí phát sinh của người cao tuổi, không phải báo cáo tài chính của viện";

    public static ReportBody of(Report r) {
        return new ReportBody(
                r.group().name(),
                r.from(),
                r.to(),
                r.asOf(),
                r.filters().toMap(),
                r.results().stream()
                        .map(MetricResult::value)
                        .map(MetricValueBody::of)
                        .toList(),
                r.group() == ReportGroup.INCURRED_COSTS ? List.of(COSTS_NOTE) : List.of());
    }
}
