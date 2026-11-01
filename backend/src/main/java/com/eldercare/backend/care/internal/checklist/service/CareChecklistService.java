package com.eldercare.backend.care.internal.checklist.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.api.spi.*;
import com.eldercare.backend.care.internal.catalog.domain.*;
import com.eldercare.backend.care.internal.catalog.persistence.*;
import com.eldercare.backend.care.internal.checklist.domain.CareChecklist;
import com.eldercare.backend.care.internal.checklist.persistence.CareChecklistStore;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.domain.*;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.*;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.*;
import com.eldercare.backend.resident.api.dto.IdentityView;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T038: ghép nguồn theo lô, không tạo công việc khi đọc checklist. */
@Service
public class CareChecklistService {
    private static final PermissionKey VIEW = new PermissionKey(Function.TASK_CHECKLIST, Operation.VIEW);
    private final CareTaskStore tasks;
    private final CareTaskService commands;
    private final CareLockStore locks;
    private final CareCatalogStore catalogs;
    private final CareChecklistStore checklist;
    private final NonPerformanceReasonStore reasons;
    private final CurrentUser user;
    private final PermissionApi permissions;
    private final ScopeApi scopes;
    private final BedAllocationApi beds;
    private final ResidentLocationApi locations;
    private final AssessmentApi assessments;
    private final ResidentApi residents;
    private final ObjectProvider<MedicationChecklistPort> medication;
    private final ObjectProvider<AlertReadPort> alerts;
    private final ParameterApi parameters;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final StatusHistoryApi history;

    public CareChecklistService(
            CareTaskStore tasks,
            CareTaskService commands,
            CareLockStore locks,
            CareCatalogStore catalogs,
            CareChecklistStore checklist,
            NonPerformanceReasonStore reasons,
            CurrentUser user,
            PermissionApi permissions,
            ScopeApi scopes,
            BedAllocationApi beds,
            ResidentLocationApi locations,
            AssessmentApi assessments,
            ResidentApi residents,
            ObjectProvider<MedicationChecklistPort> medication,
            ObjectProvider<AlertReadPort> alerts,
            ParameterApi parameters,
            BusinessTime time,
            AuditLogApi audit,
            StatusHistoryApi history) {
        this.tasks = tasks;
        this.commands = commands;
        this.locks = locks;
        this.catalogs = catalogs;
        this.checklist = checklist;
        this.reasons = reasons;
        this.user = user;
        this.permissions = permissions;
        this.scopes = scopes;
        this.beds = beds;
        this.locations = locations;
        this.assessments = assessments;
        this.residents = residents;
        this.medication = medication;
        this.alerts = alerts;
        this.parameters = parameters;
        this.time = time;
        this.audit = audit;
        this.history = history;
    }

    @Transactional(readOnly = true)
    public CareChecklist load(long shiftId) {
        if (Collections.disjoint(
                        user.roles(),
                        Set.of(Role.NURSING_HOME_MANAGER, Role.FLOOR_MANAGER, Role.NURSING_STAFF, Role.CAREGIVER))
                || !permissions.permissions(user).contains(VIEW))
            throw new PermissionDeniedException("Không có quyền mở checklist nhân viên");
        long actor =
                user.staffId().orElseThrow(() -> new PermissionDeniedException("Checklist chỉ dành cho nhân viên"));
        var shift = commands.shift(shiftId);
        var staffing = commands.staff();
        boolean member = staffing.members(shiftId).stream().anyMatch(m -> m.staffId() == actor);
        if (!shift.published()
                || (!member
                        && !permissions
                                .hasPermission(user, VIEW, commands.shiftTarget(shiftId))
                                .allowed()))
            throw new PermissionDeniedException("Ca không thuộc phạm vi của nhân viên");
        var from =
                shift.startsAt().minus(parameters.read(ParameterCode.CFG_M15_07).asDuration());
        var typeRows = catalogs.list(CareCatalogKinds.TASK_TYPES);
        var certificates = new HashMap<Long, Set<String>>();
        for (var row : typeRows) certificates.put(row.id(), row.attributes().certificates());
        var qualifications = new HashMap<Set<String>, Boolean>();
        var rows = tasks.checklist(shiftId, shift.floorAreaId(), actor, from).stream()
                .filter(t -> permissions
                        .hasPermission(user, VIEW, CareTaskService.target(t))
                        .allowed())
                .filter(t -> t.performerId() != null
                        || (member
                                && user.roles().stream()
                                        .anyMatch(r -> CareTaskValidator.roleMatches(t.performerRole(), r))
                                && certificates.containsKey(t.taskTypeId())
                                && qualifications.computeIfAbsent(
                                        certificates.get(t.taskTypeId()),
                                        c -> staffing.qualified(actor, c, time.today()))))
                .toList();
        var ids = new LinkedHashSet<Long>();
        for (var task : rows) if (task.residentId() != null) ids.add(task.residentId());
        var scope = scopes.filter(user, VIEW);
        var candidates = new LinkedHashSet<Long>(scope.residentIds());
        if (scope.facilityWide()
                || scope.floorAreaIds().contains(shift.floorAreaId())
                || !scope.roomIds().isEmpty())
            candidates.addAll(beds.residentsIn(
                    new ScopeFilter(false, Set.of(shift.floorAreaId()), Set.of(), Set.of()), time.now()));
        for (long id : candidates)
            if (staffing.assignments(id, shiftId).stream().anyMatch(a -> a.staffId() == actor)
                    && permissions
                            .hasPermission(
                                    user, VIEW, new Target("M01.RESIDENT", Long.toString(id), id, shift.floorAreaId()))
                            .allowed()) ids.add(id);
        var identities = new LinkedHashMap<Long, IdentityView>();
        for (long id : ids) identities.put(id, residents.identity(id));
        var positions = locations.current(ids);
        var notes = assessments.currentCareNote(ids);
        List<MedicationChecklistPort.DoseView> doses = List.of();
        List<AlertReadPort.AlertView> open = List.of();
        ShiftStaffPort.HandoverTasksView handover = null;
        if (user.roles().contains(Role.NURSING_STAFF)) {
            var medicationSource = medication.getIfAvailable();
            var alertSource = alerts.getIfAvailable();
            if (medicationSource == null || alertSource == null)
                throw new BusinessException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "CARE_SOURCE_UNAVAILABLE",
                        "Chưa có nguồn thuốc/cảnh báo cho checklist điều dưỡng");
            doses = medicationSource.inRange(ids, from, shift.endsAt()).stream()
                    .filter(d -> ids.contains(d.residentId()))
                    .toList();
            open = alertSource.open(ids).stream()
                    .filter(a -> ids.contains(a.residentId()))
                    .toList();
            handover = staffing.handoverTasks(actor, shiftId);
        }
        return new CareChecklist(
                shiftId,
                time.now(),
                rows,
                identities,
                positions,
                notes,
                checklist.agreements(ids, time.today()),
                reasons.list().stream()
                        .filter(r -> r.status() == CatalogStatus.ACTIVE)
                        .toList(),
                doses,
                open,
                handover);
    }

    @RequiresPermission(function = Function.TASK_CHECKLIST, operation = Operation.CONFIRM)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void claim(Target target, long id) {
        var preliminary = tasks.get(id, false);
        if (preliminary.residentId() != null) locks.lock(List.of(preliminary.residentId()));
        var before = tasks.get(id, true);
        long actor = claimant(before);
        var after = tasks.claim(before, actor);
        history.add(
                CareTaskService.HISTORY,
                id,
                new HistoryEntry<>(
                        HistoryKind.POINT,
                        before.status().name(),
                        after.status().name(),
                        null,
                        time.now(),
                        "CLAIMED",
                        user.accountId(),
                        false,
                        null,
                        after.basisType().name(),
                        after.basisId(),
                        new CareTaskHandling(after.data().content()),
                        after.floorAreaId(),
                        null));
        var old = new LinkedHashMap<String, Object>();
        old.put("performerId", before.performerId());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("CARE_TASK_CLAIMED", false))
                .entity("M04.TASK", id)
                .before(old)
                .after(Map.of("performerId", actor))
                .build());
    }

    public boolean canClaim(CareTask task) {
        if (!permissions
                .hasPermission(
                        user,
                        new PermissionKey(Function.TASK_CHECKLIST, Operation.CONFIRM),
                        CareTaskService.target(task))
                .allowed()) return false;
        try {
            claimant(task);
            return true;
        } catch (BusinessException denied) {
            return false;
        }
    }

    private long claimant(CareTask before) {
        if (!Set.of(TaskStatus.NOT_YET_DUE, TaskStatus.DUE, TaskStatus.OVERDUE).contains(before.status()))
            throw new BusinessException(HttpStatus.CONFLICT, "CARE_TASK_CLOSED", "Công việc đã đóng");
        long actor = user.staffId().orElseThrow(() -> new PermissionDeniedException("Chỉ nhân viên được nhận việc"));
        var staff = commands.staff();
        var shift = commands.shift(before.shiftId());
        boolean lead = staff.shiftLead(shift.id()).equals(user.staffId());
        if (before.performerId() != null && (!lead || before.performerId() == actor))
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_PERFORMER", "Công việc đã có người được giao");
        if (!shift.published()
                || staff.members(shift.id()).stream().noneMatch(m -> m.staffId() == actor)
                || !staff.onShift(actor, before.plannedAt()))
            throw new PermissionDeniedException("Người nhận không có tên trong ca tại thời điểm công việc");
        if (user.roles().stream().noneMatch(role -> CareTaskValidator.roleMatches(before.performerRole(), role)))
            throw new PermissionDeniedException("Vai trò không phù hợp công việc");
        var type = catalogs.list(CareCatalogKinds.TASK_TYPES).stream()
                .filter(c -> c.id() == before.taskTypeId())
                .findFirst()
                .orElseThrow(NotFoundException::new);
        if (!staff.qualified(actor, type.attributes().certificates(), time.today()))
            throw new PermissionDeniedException("Chưa đủ chứng chỉ hiện hành");
        return actor;
    }
}
