package com.cambae.backend.m14.internal.report.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m14.internal.metric.domain.Metric;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import com.cambae.backend.m14.internal.metric.domain.MetricRecord;
import com.cambae.backend.m14.internal.metric.domain.MetricSource;
import com.cambae.backend.m14.internal.metric.domain.MetricValue;
import com.cambae.backend.m14.internal.metric.domain.ReportFilters;
import com.cambae.backend.m14.internal.metric.service.MetricCatalog;
import com.cambae.backend.m14.internal.report.domain.MetricResult;
import com.cambae.backend.m14.internal.report.domain.Report;
import com.cambae.backend.m14.internal.report.domain.ReportGroup;
import com.cambae.backend.m14.internal.scope.domain.ReportScope;
import com.cambae.backend.m14.internal.scope.service.DetailRowPolicy;
import com.cambae.backend.m14.internal.scope.service.ReportPeriods.ReportPeriod;
import com.cambae.backend.m14.internal.scope.service.ReportScopeService;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.ReportChannel;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tính một báo cáo khoảng cho người xem (FR-022 → FR-073; research O-03, O-05): mỗi nhóm chỉ tiêu của báo cáo lấy phạm vi
 * riêng theo bảng FR-010, nhóm người xem không được thấy thì không có trong báo cáo; không thấy nhóm nào thì từ chối.
 * Màn hình và file xuất dùng chung kết quả này nên cùng số (FR-061).
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final MetricCatalog catalog;
    private final ReportScopeService scopes;
    private final DetailRowPolicy rows;
    private final BusinessTime time;

    public ReportService(MetricCatalog catalog, ReportScopeService scopes, DetailRowPolicy rows, BusinessTime time) {
        this.catalog = catalog;
        this.scopes = scopes;
        this.rows = rows;
        this.time = time;
    }

    public Report period(CurrentUser user, ReportGroup group, ReportPeriod period, ReportFilters filters) {
        List<MetricGroup> visible = group.metricGroups().stream()
                .filter(g -> scopes.visible(user, g, ReportChannel.REPORT))
                .toList();
        if (visible.isEmpty()) {
            // từ chối với lý do của ReportScopeService (không có khóa, hoặc không thấy nhóm nào)
            scopes.period(user, group.metricGroups().getFirst(), period.start(), period.end());
        }
        List<MetricResult> results = new ArrayList<>();
        List<ScopeFilter> rough = new ArrayList<>();
        for (MetricGroup g : visible) {
            ReportScope scope = scopes.period(user, g, period.start(), period.end());
            rough.add(scope.rough());
            for (MetricSource<?> source : catalog.sourcesIn(g)) {
                results.add(result(user, source, scope, period, filters));
            }
        }
        return new Report(group, period.from(), period.to(), time.now(), filters, union(rough), List.copyOf(results));
    }

    private <R extends MetricRecord> MetricResult result(
            CurrentUser user, MetricSource<R> source, ReportScope scope, ReportPeriod period, ReportFilters filters) {
        Metric<R> metric = source.metric();
        List<R> included = metric.included(
                source.records(scope.rough(), period.start(), period.end(), filters),
                r -> scope.covers(r) && filters.matches(r));
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

    private static ScopeFilter union(List<ScopeFilter> filters) {
        if (filters.stream().anyMatch(ScopeFilter::facilityWide)) {
            return ScopeFilter.FACILITY;
        }
        Set<Long> floors = new HashSet<>();
        Set<Long> rooms = new HashSet<>();
        Set<Long> residents = new HashSet<>();
        for (ScopeFilter f : filters) {
            floors.addAll(f.floorAreaIds());
            rooms.addAll(f.roomIds());
            residents.addAll(f.residentIds());
        }
        return new ScopeFilter(false, Set.copyOf(floors), Set.copyOf(rooms), Set.copyOf(residents));
    }
}
