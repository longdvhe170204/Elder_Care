package com.cambae.backend.m14.internal.scope.service;

import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m14.internal.metric.domain.GroupScope;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import com.cambae.backend.m14.internal.metric.domain.MetricGroupAccess;
import com.cambae.backend.m14.internal.scope.domain.ReportScope;
import com.cambae.backend.m14.internal.scope.domain.ReportScope.Grant;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.dto.FailedLayer;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.ReportChannel;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.m15.account.api.spi.AssignmentApi;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.m15.account.api.spi.InvalidPermissionDataException;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
 *   <li>Dashboard: phạm vi hiện hành của 002 ({@code ScopeApi.filter}, gồm CFG-M15-07).
 *   <li>Trưởng tầng, khoảng bất kỳ: tầng/khu đang được giao lúc xem, mọi thời điểm.
 *   <li>Điều dưỡng, Người phụ trách ca: từng phân công theo ca của 008 trong giờ ca ± CFG-M15-07; ca hủy, vắng, nghỉ có
 *       duyệt không có trong lịch sử phân công của 008 ({@code AssignmentApi.history}).
 * </ul>
 *
 * Ô toàn viện của bảng FR-010 bị 002 hạ phạm vi (FR-010a) thì tính như phạm vi phân công. Dữ liệu nguồn lỗi thì từ
 * chối với lớp {@code SOURCE_DATA} (FR-016).
 */
@Service
@Transactional(readOnly = true)
public class ReportScopeService {

    /** Dòng "Dashboard, báo cáo" của Phụ lục 27. */
    public static final PermissionKey KEY = new PermissionKey(Function.DASHBOARD_REPORT, Operation.VIEW);

    private final PermissionApi permissions;
    private final ScopeApi scopes;
    private final ObjectProvider<AssignmentApi> assignments;
    private final ParameterApi parameters;
    private final BusinessTime time;

    public ReportScopeService(
            PermissionApi permissions,
            ScopeApi scopes,
            ObjectProvider<AssignmentApi> assignments,
            ParameterApi parameters,
            BusinessTime time) {
        this.permissions = permissions;
        this.scopes = scopes;
        this.assignments = assignments;
        this.parameters = parameters;
        this.time = time;
    }

    /** Phạm vi dashboard (trong ngày, ca hiện tại). */
    public ReportScope dashboard(CurrentUser user, MetricGroup group) {
        Set<GroupScope> kinds = allowedKinds(user, group, ReportChannel.DASHBOARD);
        try {
            ScopeFilter current = scopes.filter(user, KEY);
            if (!current.facilityWide()) {
                return ReportScope.of(List.of(Grant.always(current)));
            }
            if (kinds.contains(GroupScope.FACILITY)) {
                return ReportScope.FACILITY;
            }
            // Ô toàn viện bị hạ cho riêng nhóm này (FR-010a): phân công đang góp vào phạm vi lúc xem
            AssignmentApi api = assignments.getIfAvailable();
            if (api == null || user.staffId().isEmpty()) {
                return ReportScope.EMPTY;
            }
            Instant now = time.now();
            Duration margin = parameters.read(ParameterCode.CFG_M15_07).asDuration();
            List<Grant> grants = new ArrayList<>();
            for (AssignmentView a : api.history(user.staffId().get(), now.minus(margin), now.plus(margin))) {
                Grant g = duringShift(a, margin, now);
                if (!now.isBefore(g.from()) && now.isBefore(g.to())) {
                    grants.add(new Grant(g.floorAreaIds(), g.roomIds(), g.residentIds(), null, null));
                }
            }
            return ReportScope.of(grants);
        } catch (InvalidPermissionDataException e) {
            throw sourceDataError();
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
            if (kinds.contains(GroupScope.ASSIGNED_FLOORS) || kinds.contains(GroupScope.FLOOR_STAFF)) {
                grants.add(assignedFloors(api.activeAt(staffId, time.now())));
            }
            if (kinds.contains(GroupScope.ASSIGNMENT)
                    || kinds.contains(GroupScope.ASSIGNMENT_AREA)
                    || kinds.contains(GroupScope.FACILITY)) {
                Duration margin = parameters.read(ParameterCode.CFG_M15_07).asDuration();
                for (AssignmentView a : api.history(staffId, from.minus(margin), to.plus(margin))) {
                    if (a.kind() != AssignmentKind.FLOOR_MANAGER) {
                        grants.add(duringShift(a, margin, to));
                    }
                }
            }
            return ReportScope.of(grants);
        } catch (InvalidPermissionDataException e) {
            throw sourceDataError();
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

    private static Grant assignedFloors(List<AssignmentView> active) {
        Set<Long> floors = new HashSet<>();
        for (AssignmentView a : active) {
            if (a.kind() == AssignmentKind.FLOOR_MANAGER && a.floorAreaId() != null) {
                floors.add(a.floorAreaId());
            }
        }
        return new Grant(Set.copyOf(floors), Set.of(), Set.of(), null, null);
    }

    private static Grant duringShift(AssignmentView a, Duration margin, Instant periodEnd) {
        Duration m = a.shiftId() != null ? margin : Duration.ZERO;
        Instant end = a.to() == null ? periodEnd.plus(margin) : a.to().plus(m);
        return new Grant(
                ids(a.floorAreaId()),
                ids(a.roomId()),
                ids(a.residentId()),
                a.from().minus(m),
                end);
    }

    private static Set<Long> ids(Long id) {
        return id == null ? Set.of() : Set.of(id);
    }

    private static PermissionDeniedException sourceDataError() {
        return new PermissionDeniedException(
                "PERMISSION_DENIED",
                "Không xác định được phạm vi của người xem",
                Map.of("failedLayer", FailedLayer.SOURCE_DATA.name()));
    }
}
