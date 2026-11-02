package com.eldercare.backend.reporting.internal.scope.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.ReportChannel;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.api.spi.AssignmentApi;
import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.GroupScope;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroupAccess;
import com.eldercare.backend.reporting.internal.scope.domain.ReportScope;
import com.eldercare.backend.reporting.internal.scope.domain.ReportScope.Grant;
import com.eldercare.backend.staffing.api.ShiftApi;
import com.eldercare.backend.staffing.api.dto.ShiftView;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dựng {@link ReportScope} theo FR-010, FR-012 (research O-03), xét lại mỗi lần gọi (FR-017).
 *
 * <ul>
 *   <li>Dashboard: phạm vi hiện hành của 002, gồm CFG-M15-07, chỉ từ các loại phân công ô của nhóm cho phép (FR-015).
 *   <li>Trưởng tầng, khoảng bất kỳ: tầng/khu đang được giao lúc xem, mọi thời điểm.
 *   <li>Điều dưỡng, Người phụ trách ca: từng phân công theo ca của 008 trong giờ ca ± CFG-M15-07; bị gỡ, vắng giữa ca thì
 *       dừng tại mốc đó, không cộng biên; bổ sung giữa ca thì tính từ lúc có tên; ca không còn thuộc về người xem (hủy,
 *       vắng từ đầu ca, nghỉ có duyệt) không có trong {@code ShiftApi.staffShifts} nên bị loại.
 *   <li>Việc chung, liều chung của tầng/khu trong ca (Q-61, Q-85): phân công theo người, phòng góp cả tầng/khu của ca
 *       cho bản ghi không gắn người cao tuổi.
 * </ul>
 *
 * Ô toàn viện của bảng FR-010 bị 002 hạ phạm vi (FR-010a) thì tính như phạm vi phân công. Dữ liệu nguồn lỗi thì từ
 * chối với lớp {@code SOURCE_DATA} và báo Quản lý viện (FR-016).
 */
@Service
@Transactional(readOnly = true)
public class ReportScopeService {

    /** Dòng "Dashboard, báo cáo" của Phụ lục 27. */
    public static final PermissionKey KEY = new PermissionKey(Function.DASHBOARD_REPORT, Operation.VIEW);

    private final PermissionApi permissions;
    private final ScopeApi scopes;
    private final ObjectProvider<AssignmentApi> assignments;
    private final ShiftApi shifts;
    private final ParameterApi parameters;
    private final BusinessTime time;

    public ReportScopeService(
            PermissionApi permissions,
            ScopeApi scopes,
            ObjectProvider<AssignmentApi> assignments,
            ShiftApi shifts,
            ParameterApi parameters,
            BusinessTime time) {
        this.permissions = permissions;
        this.scopes = scopes;
        this.assignments = assignments;
        this.shifts = shifts;
        this.parameters = parameters;
        this.time = time;
    }

    /** Phạm vi dashboard (trong ngày, ca hiện tại). */
    public ReportScope dashboard(CurrentUser user, MetricGroup group) {
        Set<GroupScope> kinds = allowedKinds(user, group, ReportChannel.DASHBOARD);
        try {
            if (scopes.filter(user, KEY).facilityWide() && kinds.contains(GroupScope.FACILITY)) {
                return ReportScope.FACILITY;
            }
            ScopeFilter assigned = scopes.assignedFilter(user, assignmentKinds(kinds));
            return ReportScope.of(List.of(Grant.always(assigned, commonFloorsNow(user, kinds))));
        } catch (InvalidPermissionDataException e) {
            throw sourceDataError(user, e);
        }
    }

    /** Phạm vi báo cáo khoảng {@code [from, to)}. */
    public ReportScope period(CurrentUser user, MetricGroup group, Instant from, Instant to) {
        Set<GroupScope> kinds = allowedKinds(user, group, ReportChannel.REPORT);
        try {
            if (kinds.contains(GroupScope.FACILITY) && scopes.filter(user, KEY).facilityWide()) {
                return ReportScope.FACILITY;
            }
            AssignmentApi api = assignments.getIfAvailable();
            if (api == null || user.staffId().isEmpty()) {
                return ReportScope.EMPTY;
            }
            long staffId = user.staffId().get();
            List<Grant> grants = new ArrayList<>();
            Set<AssignmentKind> assignmentKinds = assignmentKinds(kinds);
            if (assignmentKinds.contains(AssignmentKind.FLOOR_MANAGER)) {
                grants.add(assignedFloors(api.activeAt(staffId, time.now())));
            }
            if (assignmentKinds.contains(AssignmentKind.SHIFT_MEMBER)) {
                Duration margin = parameters.read(ParameterCode.CFG_M15_07).asDuration();
                Instant start = from.minus(margin);
                Instant end = to.plus(margin);
                Map<Long, ShiftView> belonged = shiftsOf(staffId, start, end);
                for (AssignmentView a : api.history(staffId, start, end)) {
                    if (a.kind() != AssignmentKind.FLOOR_MANAGER
                            && (a.shiftId() == null || belonged.containsKey(a.shiftId()))) {
                        grants.add(duringShift(a, belonged.get(a.shiftId()), margin, to));
                    }
                }
            }
            return ReportScope.of(grants);
        } catch (InvalidPermissionDataException e) {
            throw sourceDataError(user, e);
        }
    }

    /**
     * Người xem thấy nhóm chỉ tiêu ở kênh đã cho (bảng FR-010 sau thu hẹp của 002), không ném lỗi: báo cáo bỏ nhóm không
     * thấy mà không làm hỏng transaction của lệnh xuất.
     */
    public boolean visible(CurrentUser user, MetricGroup group, ReportChannel channel) {
        return permissions.permissions(user).contains(KEY)
                && !MetricGroupAccess.scopes(user.roles(), group, channel, permissions.metricGroupRestrictions(user))
                        .isEmpty();
    }

    private Set<GroupScope> allowedKinds(CurrentUser user, MetricGroup group, ReportChannel channel) {
        if (!permissions.permissions(user).contains(KEY)) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED", "Không có quyền xem dashboard, báo cáo", Map.of("failedLayer", "ROLE"));
        }
        Set<GroupScope> kinds =
                MetricGroupAccess.scopes(user.roles(), group, channel, permissions.metricGroupRestrictions(user));
        if (kinds.isEmpty()) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED", "Không có quyền xem nhóm chỉ tiêu", Map.of("failedLayer", "ROLE"));
        }
        return kinds;
    }

    /** Loại phân công góp vào ô của nhóm; ô toàn viện chỉ tới đây khi bị hạ phạm vi (FR-010a). */
    private static Set<AssignmentKind> assignmentKinds(Set<GroupScope> kinds) {
        Set<AssignmentKind> result = EnumSet.noneOf(AssignmentKind.class);
        if (kinds.contains(GroupScope.ASSIGNED_FLOORS) || kinds.contains(GroupScope.FLOOR_STAFF)) {
            result.add(AssignmentKind.FLOOR_MANAGER);
        }
        if (kinds.contains(GroupScope.ASSIGNMENT)
                || kinds.contains(GroupScope.ASSIGNMENT_AREA)
                || kinds.contains(GroupScope.FACILITY)) {
            result.add(AssignmentKind.SHIFT_MEMBER);
            result.add(AssignmentKind.SHIFT_LEAD);
        }
        return result;
    }

    private Map<Long, ShiftView> shiftsOf(long staffId, Instant from, Instant to) {
        Map<Long, ShiftView> result = new HashMap<>();
        for (ShiftView s : shifts.staffShifts(staffId, from, to)) {
            result.put(s.id(), s);
        }
        return result;
    }

    /** Tầng/khu của ca người xem đang thuộc về lúc xem, ± CFG-M15-07, cho việc chung, liều chung. */
    private Set<Long> commonFloorsNow(CurrentUser user, Set<GroupScope> kinds) {
        AssignmentApi api = assignments.getIfAvailable();
        if (api == null || user.staffId().isEmpty() || !assignmentKinds(kinds).contains(AssignmentKind.SHIFT_MEMBER)) {
            return Set.of();
        }
        long staffId = user.staffId().get();
        Instant now = time.now();
        Duration margin = parameters.read(ParameterCode.CFG_M15_07).asDuration();
        Instant start = now.minus(margin);
        Instant end = now.plus(margin);
        Map<Long, ShiftView> belonged = shiftsOf(staffId, start, end);
        Set<Long> floors = new HashSet<>();
        for (AssignmentView a : api.history(staffId, start, end)) {
            Grant g = duringShift(a, belonged.get(a.shiftId()), margin, now);
            if (!now.isBefore(g.from()) && now.isBefore(g.to())) {
                floors.addAll(g.commonFloorAreaIds());
            }
        }
        return Set.copyOf(floors);
    }

    private static Grant assignedFloors(List<AssignmentView> active) {
        Set<Long> floors = new HashSet<>();
        for (AssignmentView a : active) {
            if (a.kind() == AssignmentKind.FLOOR_MANAGER && a.floorAreaId() != null) {
                floors.add(a.floorAreaId());
            }
        }
        return new Grant(Set.copyOf(floors), Set.of(), Set.of(), Set.of(), null, null);
    }

    private static Grant duringShift(AssignmentView a, ShiftView shift, Duration margin, Instant periodEnd) {
        Duration m = a.shiftId() != null ? margin : Duration.ZERO;
        Instant start = shift != null && a.from().isAfter(shift.startsAt())
                ? a.from()
                : a.from().minus(m);
        Instant end;
        if (a.scopeEndsAt() != null) {
            end = a.scopeEndsAt();
        } else {
            end = a.to() == null ? periodEnd.plus(margin) : a.to().plus(m);
        }
        boolean withFloor = a.kind() == AssignmentKind.SHIFT_MEMBER && a.floorAreaId() == null && shift != null;
        return new Grant(
                ids(a.floorAreaId()),
                ids(a.roomId()),
                ids(a.residentId()),
                withFloor ? ids(shift.floorAreaId()) : Set.of(),
                start,
                end);
    }

    private static Set<Long> ids(Long id) {
        return id == null ? Set.of() : Set.of(id);
    }

    private PermissionDeniedException sourceDataError(CurrentUser user, InvalidPermissionDataException error) {
        permissions.recordDataError(user, error);
        return new PermissionDeniedException(
                "PERMISSION_DENIED",
                "Không xác định được phạm vi của người xem",
                Map.of("failedLayer", FailedLayer.SOURCE_DATA.name()));
    }
}
