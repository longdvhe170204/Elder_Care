package com.eldercare.backend.staffing.internal.swap.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.api.SwapStatisticsApi;
import com.eldercare.backend.staffing.api.dto.StaffRequestView;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveDetail;
import com.eldercare.backend.staffing.internal.swap.domain.SwapDetail;
import com.eldercare.backend.staffing.internal.swap.domain.SwapStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 015 cung cấp cho 016 (FR-042): số yêu cầu theo trạng thái cuối, thời gian lập → quyết định, giờ làm theo tháng. */
@Service
class SwapStatistics implements SwapStatisticsApi {

    private static final Set<String> ALL = Set.of(
            SwapStatus.AWAITING_RECIPIENT,
            SwapStatus.PENDING_APPROVAL,
            SwapStatus.APPLIED,
            SwapStatus.PARTIALLY_APPLIED,
            SwapStatus.REJECTED,
            SwapStatus.CANCELLED,
            SwapStatus.RECIPIENT_DECLINED,
            SwapStatus.EXPIRED);

    private final SwapRecords records;
    private final ShiftStore shifts;
    private final BusinessTime time;

    SwapStatistics(SwapRecords records, ShiftStore shifts, BusinessTime time) {
        this.records = records;
        this.shifts = shifts;
        this.time = time;
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<StaffRequestView> requests(ScopeFilter scope, LocalDate from, LocalDate to) {
        Instant start = time.startOfDay(from);
        Instant end = time.startOfDay(to.plusDays(1));
        List<StaffRequestView> out = new ArrayList<>();
        for (SimpleRecord<SwapDetail> row : records.swaps(ALL)) {
            Long floor = shifts.find(row.content().shiftXId())
                    .map(WorkShift::scopeId)
                    .orElse(null);
            if (inRange(row.createdAt(), start, end) && inScope(scope, floor)) {
                out.add(new StaffRequestView(
                        row.content().requestId(),
                        SwapHandler.TYPE,
                        row.status(),
                        floor,
                        row.createdAt(),
                        row.content().decidedAt()));
            }
        }
        for (SimpleRecord<LeaveDetail> row : records.leaves(ALL)) {
            LeaveDetail d = row.content();
            Long floor = "FLOOR".equals(d.scopeType()) || "AREA".equals(d.scopeType()) ? d.scopeId() : null;
            if (inRange(row.createdAt(), start, end) && inScope(scope, floor)) {
                out.add(new StaffRequestView(
                        d.requestId(), LeaveHandler.TYPE, row.status(), floor, row.createdAt(), d.decidedAt()));
            }
        }
        out.sort(Comparator.comparingLong(StaffRequestView::requestId));
        return out.stream();
    }

    private static boolean inRange(Instant at, Instant from, Instant to) {
        return !at.isBefore(from) && at.isBefore(to);
    }

    private static boolean inScope(ScopeFilter scope, Long floor) {
        return scope.facilityWide() || floor != null && scope.floorAreaIds().contains(floor);
    }
}
