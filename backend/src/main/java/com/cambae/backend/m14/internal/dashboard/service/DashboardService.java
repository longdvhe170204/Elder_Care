package com.cambae.backend.m14.internal.dashboard.service;

import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m14.internal.metric.domain.Metric;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import com.cambae.backend.m14.internal.metric.domain.MetricSource;
import com.cambae.backend.m14.internal.metric.domain.MetricValue;
import com.cambae.backend.m14.internal.metric.service.MetricCatalog;
import com.cambae.backend.m14.internal.metric.service.MetricEvaluator;
import com.cambae.backend.m14.internal.scope.domain.ReportScope;
import com.cambae.backend.m14.internal.scope.service.ReportScopeService;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.dto.ReportChannel;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dashboard theo vai trò và phạm vi hiện hành (FR-020, FR-021; research O-06): một khối cho mỗi nhóm chỉ tiêu của bảng
 * FR-020 người xem được thấy ở kênh dashboard; khối có phạm vi rỗng thì bỏ, không còn khối nào thì "Hiện không có phạm vi
 * phân công". Số liệu "trong ngày": nguồn nhận khoảng hôm nay, nguồn "lúc xem" bỏ qua khoảng. Mỗi khối đệm 20 giây theo
 * (nhóm, phạm vi), không chia sẻ giữa người có phạm vi khác nhau.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    static final String NO_SCOPE = "Hiện không có phạm vi phân công";

    /** Nhóm chỉ tiêu của bảng FR-020, theo thứ tự dòng. */
    static final List<MetricGroup> GROUPS = List.of(
            MetricGroup.RESIDENTS,
            MetricGroup.BEDS,
            MetricGroup.CARE_TASKS,
            MetricGroup.HEALTH_EVENTS,
            MetricGroup.ACTIVITIES,
            MetricGroup.INCURRED_COSTS,
            MetricGroup.BALANCES,
            MetricGroup.RESIDENCE_REGISTRATION,
            MetricGroup.ASSETS,
            MetricGroup.CRITICAL_FLAGS,
            MetricGroup.FACILITY_LICENSES,
            MetricGroup.STAFF_LICENSES,
            MetricGroup.STAFFING_RATIO,
            MetricGroup.CONTAINMENT_ZONES,
            MetricGroup.HANDOVERS);

    public enum BlockStatus {
        COMPLETE,
        UPDATING
    }

    public record Block(MetricGroup group, BlockStatus status, List<MetricValue> values) {}

    /** {@code asOf} là lúc lấy số của khối cũ nhất (khối đệm giữ thời điểm đã tính). */
    public record Dashboard(Instant asOf, List<Block> blocks, String message) {}

    private record CacheKey(MetricGroup group, ReportScope scope) {}

    private record Cached(Instant at, List<MetricValue> values) {}

    private final Map<CacheKey, Cached> cache = new ConcurrentHashMap<>();

    private final MetricCatalog catalog;
    private final ReportScopeService scopes;
    private final MetricEvaluator evaluator;
    private final PermissionApi permissions;
    private final BusinessTime time;

    /** Thời gian đệm của một khối (research O-06; thông số kỹ thuật, không phải CFG). */
    private final Duration cacheFor;

    public DashboardService(
            MetricCatalog catalog,
            ReportScopeService scopes,
            MetricEvaluator evaluator,
            PermissionApi permissions,
            BusinessTime time,
            @Value("${eldercare.dashboard.cache:PT20S}") Duration cacheFor) {
        this.catalog = catalog;
        this.scopes = scopes;
        this.evaluator = evaluator;
        this.permissions = permissions;
        this.time = time;
        this.cacheFor = cacheFor;
    }

    public Dashboard dashboard(CurrentUser user) {
        if (!permissions.permissions(user).contains(ReportScopeService.KEY)) {
            throw new PermissionDeniedException("Không có quyền xem dashboard, báo cáo");
        }
        Instant now = time.now();
        LocalDate today = time.today();
        Instant from = time.startOfDay(today);
        Instant to = time.startOfDay(today.plusDays(1));
        Instant asOf = now;
        cache.values().removeIf(c -> c.at().isBefore(now.minus(cacheFor)));
        List<Block> blocks = new ArrayList<>();
        for (MetricGroup group : GROUPS) {
            if (!scopes.visible(user, group, ReportChannel.DASHBOARD)) {
                continue;
            }
            ReportScope scope = scopes.dashboard(user, group);
            if (scope.isEmpty()) {
                continue;
            }
            Cached cached = cache.compute(new CacheKey(group, scope), (key, old) -> {
                if (old != null
                        && old.at().isAfter(now.minus(cacheFor))
                        && !old.at().isAfter(now)) {
                    return old;
                }
                List<MetricValue> values = new ArrayList<>();
                for (MetricSource<?> source : catalog.sourcesIn(group)) {
                    values.add(evaluator.current(source, scope, from, to));
                }
                return new Cached(now, List.copyOf(values));
            });
            if (cached.at().isBefore(asOf)) {
                asOf = cached.at();
            }
            blocks.add(new Block(group, BlockStatus.COMPLETE, cached.values()));
        }
        return new Dashboard(asOf, List.copyOf(blocks), blocks.isEmpty() ? NO_SCOPE : null);
    }

    /** Chỉ tiêu người xem thấy ở dashboard hoặc báo cáo, kèm định nghĩa hiển thị tại chỗ (FR-003, FR-010). */
    public List<Metric<?>> metrics(CurrentUser user) {
        if (!permissions.permissions(user).contains(ReportScopeService.KEY)) {
            return List.of();
        }
        Set<MetricGroup> groups = EnumSet.noneOf(MetricGroup.class);
        for (MetricGroup g : MetricGroup.values()) {
            if (scopes.visible(user, g, ReportChannel.DASHBOARD) || scopes.visible(user, g, ReportChannel.REPORT)) {
                groups.add(g);
            }
        }
        return catalog.inGroups(groups);
    }
}
