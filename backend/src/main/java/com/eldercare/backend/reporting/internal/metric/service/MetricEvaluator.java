package com.eldercare.backend.reporting.internal.metric.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.report.domain.MetricResult;
import com.eldercare.backend.reporting.internal.scope.domain.ReportScope;
import com.eldercare.backend.reporting.internal.scope.service.DetailRowPolicy;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tính một chỉ tiêu trên phạm vi và khoảng (lọc hai bước của research O-03): nguồn lọc thô, rồi {@link ReportScope#covers}
 * và bộ lọc chung lọc từng bản ghi. Dashboard, báo cáo, danh sách chi tiết và file xuất cùng đi qua đây nên số trên màn
 * hình bằng số dòng chi tiết (FR-009).
 */
@Service
@Transactional(readOnly = true)
public class MetricEvaluator {

    private final DetailRowPolicy rows;

    public MetricEvaluator(DetailRowPolicy rows) {
        this.rows = rows;
    }

    public MetricValue value(
            MetricSource<?> source, ReportScope scope, Instant from, Instant to, ReportFilters filters) {
        return valueOf(capture(source), scope, from, to, filters);
    }

    /** Giá trị, người cao tuổi theo ô, dòng chi tiết đã giới hạn trường (báo cáo, xuất). */
    public MetricResult result(
            CurrentUser user,
            MetricSource<?> source,
            ReportScope scope,
            Instant from,
            Instant to,
            ReportFilters filters) {
        return resultOf(user, capture(source), scope, from, to, filters);
    }

    /** Dòng chi tiết của con số, hoặc của một ô {@code key}, đã giới hạn trường (FR-009, FR-014). */
    public List<DetailRow> details(
            CurrentUser user,
            MetricSource<?> source,
            ReportScope scope,
            Instant from,
            Instant to,
            ReportFilters filters,
            String key) {
        return detailsOf(user, capture(source), scope, from, to, filters, key);
    }

    /** Giá trị "lúc xem" của dashboard ({@link MetricSource#current}). */
    public MetricValue current(MetricSource<?> source, ReportScope scope, Instant from, Instant to) {
        return currentOf(capture(source), scope, from, to);
    }

    /** Dòng chi tiết của con số dashboard, đã giới hạn trường. */
    public List<DetailRow> currentDetails(
            CurrentUser user, MetricSource<?> source, ReportScope scope, Instant from, Instant to, String key) {
        return currentDetailsOf(user, capture(source), scope, from, to, key);
    }

    @SuppressWarnings("unchecked")
    private static <R extends MetricRecord> MetricSource<R> capture(MetricSource<?> source) {
        return (MetricSource<R>) source;
    }

    private static <R extends MetricRecord> List<R> included(
            MetricSource<R> source, ReportScope scope, Instant from, Instant to, ReportFilters filters) {
        return source.metric()
                .included(source.records(scope.rough(), from, to, filters), r -> scope.covers(r) && filters.matches(r));
    }

    private static <R extends MetricRecord> List<R> currentIncluded(
            MetricSource<R> source, ReportScope scope, Instant from, Instant to) {
        return source.metric().included(source.current(scope.rough(), from, to), scope::covers);
    }

    private <R extends MetricRecord> MetricValue currentOf(
            MetricSource<R> source, ReportScope scope, Instant from, Instant to) {
        return source.metric().compute(currentIncluded(source, scope, from, to));
    }

    private <R extends MetricRecord> List<DetailRow> currentDetailsOf(
            CurrentUser user, MetricSource<R> source, ReportScope scope, Instant from, Instant to, String key) {
        return rows.apply(user, source.metric().details(currentIncluded(source, scope, from, to), key));
    }

    private <R extends MetricRecord> MetricValue valueOf(
            MetricSource<R> source, ReportScope scope, Instant from, Instant to, ReportFilters filters) {
        return source.metric().compute(included(source, scope, from, to, filters));
    }

    private <R extends MetricRecord> MetricResult resultOf(
            CurrentUser user,
            MetricSource<R> source,
            ReportScope scope,
            Instant from,
            Instant to,
            ReportFilters filters) {
        Metric<R> metric = source.metric();
        List<R> included = included(source, scope, from, to, filters);
        MetricValue value = metric.compute(included);
        Map<String, Set<Long>> byKey = new LinkedHashMap<>();
        for (MetricValue.Breakdown b : value.breakdown()) {
            byKey.put(b.key(), metric.residents(included, b.key()));
        }
        return new MetricResult(
                metric.code(),
                metric.name(),
                value,
                metric.residents(included, null),
                byKey,
                rows.apply(user, metric.details(included, null)));
    }

    private <R extends MetricRecord> List<DetailRow> detailsOf(
            CurrentUser user,
            MetricSource<R> source,
            ReportScope scope,
            Instant from,
            Instant to,
            ReportFilters filters,
            String key) {
        return rows.apply(user, source.metric().details(included(source, scope, from, to, filters), key));
    }
}
