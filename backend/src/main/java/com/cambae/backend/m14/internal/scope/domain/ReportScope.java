package com.cambae.backend.m14.internal.scope.domain;

import com.cambae.backend.m14.internal.metric.domain.MetricRecord;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Phạm vi báo cáo của người xem cho một nhóm chỉ tiêu ({@code PhamViBaoCao}; FR-012, research O-03). Lọc hai bước:
 * {@link #rough()} là bộ lọc thô truyền cho {@code *ThongKeApi}, {@link #covers} lọc chính xác từng bản ghi theo khoảng
 * thời gian của từng phần phạm vi. Tài khoản nhiều vai trò là hợp các phần (FR-015).
 */
public record ReportScope(boolean facilityWide, List<Grant> grants) {

    public static final ReportScope FACILITY = new ReportScope(true, List.of());

    public static final ReportScope EMPTY = new ReportScope(false, List.of());

    /**
     * Một phần phạm vi: tầng/khu, phòng, người cao tuổi, trong khoảng {@code [from, to)} của mốc bản ghi; khoảng rỗng là
     * mọi thời điểm (trưởng tầng, dashboard).
     */
    public record Grant(Set<Long> floorAreaIds, Set<Long> roomIds, Set<Long> residentIds, Instant from, Instant to) {

        public static Grant always(ScopeFilter filter) {
            return new Grant(filter.floorAreaIds(), filter.roomIds(), filter.residentIds(), null, null);
        }

        boolean matches(MetricRecord r) {
            if (from != null) {
                Instant t = r.referenceTime();
                if (t == null || t.isBefore(from) || !t.isBefore(to)) {
                    return false;
                }
            }
            return (r.floorAreaId() != null && floorAreaIds.contains(r.floorAreaId()))
                    || (r.roomId() != null && roomIds.contains(r.roomId()))
                    || (r.residentId() != null && residentIds.contains(r.residentId()));
        }
    }

    public static ReportScope of(List<Grant> grants) {
        return new ReportScope(false, List.copyOf(grants));
    }

    /** Hợp các phần, bỏ khoảng thời gian (bước 1). */
    public ScopeFilter rough() {
        if (facilityWide) {
            return ScopeFilter.FACILITY;
        }
        Set<Long> floors = new HashSet<>();
        Set<Long> rooms = new HashSet<>();
        Set<Long> residents = new HashSet<>();
        for (Grant g : grants) {
            floors.addAll(g.floorAreaIds());
            rooms.addAll(g.roomIds());
            residents.addAll(g.residentIds());
        }
        return new ScopeFilter(false, Set.copyOf(floors), Set.copyOf(rooms), Set.copyOf(residents));
    }

    /** Bản ghi thuộc phạm vi (bước 2). */
    public boolean covers(MetricRecord r) {
        return facilityWide || grants.stream().anyMatch(g -> g.matches(r));
    }

    /** Không có phần nào: dashboard hiện "hiện không có phạm vi phân công" (FR-020). */
    public boolean isEmpty() {
        return !facilityWide
                && grants.stream()
                        .allMatch(g -> g.floorAreaIds().isEmpty()
                                && g.roomIds().isEmpty()
                                && g.residentIds().isEmpty());
    }
}
