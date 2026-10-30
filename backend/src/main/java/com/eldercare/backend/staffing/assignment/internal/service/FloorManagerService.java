package com.eldercare.backend.staffing.assignment.internal.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.staffing.assignment.internal.domain.FloorManagerAssignment;
import com.eldercare.backend.staffing.assignment.internal.persistence.AssignmentStore;
import com.eldercare.backend.staffing.staff.internal.persistence.StaffStore;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-024: ngày kết thúc bao gồm cả ngày; khóa nhân viên trước rồi khóa tầng. */
@Service
public class FloorManagerService {
    private final AssignmentStore store;
    private final StaffStore staff;
    private final AssignmentSources sources;
    private final AccountApi accounts;
    private final ObjectProvider<CurrentUser> users;
    private final PermissionApi permissions;
    private final BusinessTime time;
    private final AuditLogApi audit;

    public FloorManagerService(
            AssignmentStore store,
            StaffStore staff,
            AssignmentSources sources,
            AccountApi accounts,
            ObjectProvider<CurrentUser> users,
            PermissionApi permissions,
            BusinessTime time,
            AuditLogApi audit) {
        this.store = store;
        this.staff = staff;
        this.sources = sources;
        this.accounts = accounts;
        this.users = users;
        this.permissions = permissions;
        this.time = time;
        this.audit = audit;
    }

    public List<FloorManagerAssignment> list(Long floor) {
        return store.managers(floor, false).stream()
                .filter(row -> allowed(Operation.VIEW, row.floorAreaId()))
                .toList();
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.CREATE)
    @Transactional
    public long assign(long person, long floor, LocalDate from, LocalDate to, Reason reason, Target target) {
        managerOnly();
        staff.lock(person);
        store.lockFloors(Set.of(floor));
        if (from == null || from.isBefore(time.today()) || to != null && to.isBefore(from))
            throw invalid("Khoảng giao Trưởng tầng không hợp lệ");
        if (!sources.structure().active(new LocationRef("FLOOR", floor))
                && !sources.structure().active(new LocationRef("AREA", floor)))
            throw invalid("Tầng/khu vực không còn hiệu lực");
        var profile = staff.find(person).orElseThrow(NotFoundException::new);
        var account = accounts.byStaff(person).orElseThrow(() -> invalid("Nhân viên chưa có tài khoản"));
        if (!"WORKING".equals(profile.status())
                || account.status() != AccountStatus.ACTIVE
                || !account.roles().contains(Role.FLOOR_MANAGER))
            throw invalid("Người được giao phải đang làm việc, tài khoản Hoạt động và có vai trò Trưởng tầng");
        Instant start = time.startOfDay(from), end = to == null ? null : time.startOfDay(to.plusDays(1));
        if (store.managers(floor, true).stream()
                .anyMatch(a -> (end == null || a.startsAt().isBefore(end))
                        && (a.endsAt() == null || a.endsAt().isAfter(start))))
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "FLOOR_MANAGER_ASSIGNMENT_OVERLAP",
                    "Tầng đã có giao Trưởng tầng chồng thời gian");
        long id = store.assignFloor(
                person,
                floor,
                start,
                end,
                reason == null ? null : reason.value(),
                users.getObject().accountId(),
                time.now());
        log("FLOOR_MANAGER_ASSIGNED", id, Map.of(), Map.of("staffId", person, "floorAreaId", floor), reason);
        return id;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.SHIFT_SCHEDULE, operation = Operation.UPDATE)
    @Transactional
    public void end(long id, LocalDate to, Reason reason, Target target) {
        managerOnly();
        var initial = store.manager(id, false);
        staff.lock(initial.staffId());
        store.lockFloors(Set.of(initial.floorAreaId()));
        var row = store.manager(id, true);
        if (!"ACTIVE".equals(row.status()) || to == null || to.isBefore(time.today()))
            throw invalid("Chỉ rút ngắn giao đang có, không ghi lùi ngày");
        Instant end = time.startOfDay(to.plusDays(1));
        if (!end.isAfter(row.startsAt()) || row.endsAt() != null && end.isAfter(row.endsAt()))
            throw invalid("Ngày kết thúc phải nằm trong khoảng giao hiện có");
        store.shorten(row, end, !end.isAfter(time.now()), reason.value());
        log(
                "FLOOR_MANAGER_ASSIGNMENT_ENDED",
                id,
                Map.of("endsAt", String.valueOf(row.endsAt())),
                Map.of("endsAt", end),
                reason);
    }

    public Target target(long id) {
        var row = store.manager(id, false);
        return new Target("M09.FLOOR_MANAGER_ASSIGNMENT", Long.toString(id), null, row.floorAreaId());
    }

    private boolean allowed(Operation op, long floor) {
        return permissions
                .hasPermission(
                        users.getObject(),
                        new PermissionKey(Function.SHIFT_SCHEDULE, op),
                        new Target("M09.FLOOR_MANAGER_ASSIGNMENT", null, null, floor))
                .allowed();
    }

    private void managerOnly() {
        if (!users.getObject().roles().contains(Role.NURSING_HOME_MANAGER))
            throw new PermissionDeniedException("Chỉ Quản lý viện giao hoặc kết thúc Trưởng tầng");
    }

    private void log(String action, long id, Map<String, Object> before, Map<String, Object> after, Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, false))
                .entity("M09.FLOOR_MANAGER_ASSIGNMENT", id)
                .before(before)
                .after(after)
                .reason(reason)
                .details(Map.of("basis", "FR-024"))
                .build());
    }

    private static BusinessException invalid(String detail) {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_FLOOR_MANAGER_ASSIGNMENT", detail);
    }
}
