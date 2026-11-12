package com.eldercare.backend.reporting.internal.report.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.ReportChannel;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.incident.service.IncidentRecords;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.metric.domain.ValueLabel;
import com.eldercare.backend.reporting.internal.metric.service.MetricCatalog;
import com.eldercare.backend.reporting.internal.metric.service.MetricEvaluator;
import com.eldercare.backend.reporting.internal.metric.service.PurgedDataTypes;
import com.eldercare.backend.reporting.internal.report.domain.MetricResult;
import com.eldercare.backend.reporting.internal.report.domain.Report;
import com.eldercare.backend.reporting.internal.report.domain.ReportFilterOptions;
import com.eldercare.backend.reporting.internal.report.domain.ReportGroup;
import com.eldercare.backend.reporting.internal.scope.domain.ReportScope;
import com.eldercare.backend.reporting.internal.scope.service.ReportPeriods.ReportPeriod;
import com.eldercare.backend.reporting.internal.scope.service.ReportScopeService;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.dto.AreaView;
import com.eldercare.backend.roombedallocation.api.dto.FloorView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tính một báo cáo khoảng cho người xem (FR-022 → FR-073; research O-03, O-05): mỗi nhóm chỉ tiêu của báo cáo lấy phạm vi
 * riêng theo bảng FR-010, nhóm người xem không được thấy thì không có trong báo cáo; không thấy nhóm nào thì từ chối.
 * Khoảng chứa hôm nay thì mọi giá trị mang nhãn "ngày chưa kết thúc" (FR-007). Màn hình và file xuất dùng chung kết quả
 * này nên cùng số (FR-061).
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final MetricCatalog catalog;
    private final ReportScopeService scopes;
    private final MetricEvaluator evaluator;
    private final LocationStructureApi structure;
    private final BusinessTime time;
    private final PurgedDataTypes purged;
    private final IncidentRecords incidents;

    public ReportService(
            MetricCatalog catalog,
            ReportScopeService scopes,
            MetricEvaluator evaluator,
            LocationStructureApi structure,
            BusinessTime time,
            PurgedDataTypes purged,
            IncidentRecords incidents) {
        this.purged = purged;
        this.catalog = catalog;
        this.scopes = scopes;
        this.evaluator = evaluator;
        this.structure = structure;
        this.time = time;
        this.incidents = incidents;
    }

    public Report period(CurrentUser user, ReportGroup group, ReportPeriod period, ReportFilters filters) {
        List<MetricGroup> visible = group.metricGroups().stream()
                .filter(g -> scopes.visible(user, g, ReportChannel.REPORT))
                .toList();
        if (visible.isEmpty()) {
            // từ chối với lý do của ReportScopeService (không có khóa, hoặc không thấy nhóm nào)
            scopes.period(user, group.metricGroups().getFirst(), period.start(), period.end());
        }
        Instant now = time.now();
        boolean dayNotEnded = period.end().isAfter(now);
        List<MetricResult> results = new ArrayList<>();
        List<ScopeFilter> rough = new ArrayList<>();
        Integer cancelledIncidents = null;
        for (MetricGroup g : visible) {
            ReportScope scope = scopes.period(user, g, period.start(), period.end());
            rough.add(scope.rough());
            if (g == MetricGroup.HEALTH_EVENTS) {
                cancelledIncidents = incidents.cancelledCount(scope.rough(), period.start(), period.end());
            }
            for (MetricSource<?> source : catalog.sourcesIn(g)) {
                MetricResult r = evaluator.result(user, source, scope, period.start(), period.end(), filters);
                MetricResult labelled =
                        withPurged(r, purged.affecting(source.metric().source(), period.start()));
                // FR-051: chế độ tạm tính gắn nhãn "tạm tính, chưa chốt" cho chỉ tiêu chi phí phát sinh
                if (g == MetricGroup.INCURRED_COSTS && filters.costMode() == ReportFilters.CostMode.PROVISIONAL)
                    labelled = withLabel(labelled, ValueLabel.PROVISIONAL);
                results.add(dayNotEnded ? withDayNotEnded(labelled) : labelled);
            }
        }
        ScopeFilter scope = union(rough);
        return new Report(
                group,
                period.from(),
                period.to(),
                now,
                filters,
                scope,
                filterOptions(scope),
                List.copyOf(results),
                cancelledIncidents);
    }

    private ReportFilterOptions filterOptions(ScopeFilter scope) {
        List<ReportFilterOptions.FloorArea> floors = new ArrayList<>();
        scope.floorAreaIds().stream()
                .sorted()
                .forEach(id -> name(id).ifPresent(name -> floors.add(new ReportFilterOptions.FloorArea(id, name))));
        return new ReportFilterOptions(scope.facilityWide(), List.copyOf(floors));
    }

    private Optional<String> name(long floorAreaId) {
        return structure
                .floor(floorAreaId)
                .map(FloorView::name)
                .or(() -> structure.area(floorAreaId).map(AreaView::name));
    }

    private static MetricResult withLabel(MetricResult r, ValueLabel label) {
        MetricValue v = r.value();
        Set<ValueLabel> labels = EnumSet.of(label);
        labels.addAll(v.labels());
        MetricValue labelled = new MetricValue(
                v.metricCode(),
                v.value(),
                v.numerator(),
                v.denominator(),
                Set.copyOf(labels),
                v.pendingReviewCount(),
                v.breakdown(),
                v.purgedDataTypes());
        return new MetricResult(r.metricCode(), r.metricName(), labelled, r.residents(), r.residentsByKey(), r.rows());
    }

    private static MetricResult withDayNotEnded(MetricResult r) {
        MetricValue v = r.value();
        Set<ValueLabel> labels = EnumSet.of(ValueLabel.DAY_NOT_ENDED);
        labels.addAll(v.labels());
        MetricValue labelled = new MetricValue(
                v.metricCode(),
                v.value(),
                v.numerator(),
                v.denominator(),
                Set.copyOf(labels),
                v.pendingReviewCount(),
                v.breakdown(),
                v.purgedDataTypes());
        return new MetricResult(r.metricCode(), r.metricName(), labelled, r.residents(), r.residentsByKey(), r.rows());
    }

    /** FR-007 (T109): khoảng giao với dữ liệu đã loại bỏ theo NFR-07 thì nhãn kèm loại dữ liệu bị ảnh hưởng. */
    private static MetricResult withPurged(MetricResult r, List<String> types) {
        if (types.isEmpty()) {
            return r;
        }
        MetricValue v = r.value();
        Set<ValueLabel> labels = EnumSet.of(ValueLabel.PURGED_BY_RETENTION);
        labels.addAll(v.labels());
        MetricValue labelled = new MetricValue(
                v.metricCode(),
                v.value(),
                v.numerator(),
                v.denominator(),
                Set.copyOf(labels),
                v.pendingReviewCount(),
                v.breakdown(),
                types);
        return new MetricResult(r.metricCode(), r.metricName(), labelled, r.residents(), r.residentsByKey(), r.rows());
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
