package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.care.api.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort;
import com.eldercare.backend.care.internal.careplan.service.CarePlanQueries;
import com.eldercare.backend.care.internal.task.domain.*;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CareTaskQueries {
    private final CareTaskStore tasks;
    private final CareTaskService commands;
    private final ResidentApi residents;
    private final BedAllocationApi beds;
    private final CarePlanQueries plans;
    private final List<TaskSourcePort> sources;
    private final PermissionApi permissions;
    private final CurrentUser user;

    public CareTaskQueries(
            CareTaskStore tasks,
            CareTaskService commands,
            ResidentApi residents,
            BedAllocationApi beds,
            CarePlanQueries plans,
            List<TaskSourcePort> sources,
            PermissionApi permissions,
            CurrentUser user) {
        this.tasks = tasks;
        this.commands = commands;
        this.residents = residents;
        this.beds = beds;
        this.plans = plans;
        this.sources = List.copyOf(sources);
        this.permissions = permissions;
        this.user = user;
    }

    @RequiresPermission(function = Function.SHIFT_TASK_PLANNING, operation = Operation.VIEW)
    public List<CareTask> inShift(Target target, long id) {
        return tasks.inShift(id);
    }

    @RequiresPermission(function = Function.TASK_CHECKLIST, operation = Operation.VIEW)
    public CareTask get(Target target, long id) {
        return tasks.get(id, false);
    }

    @RequiresPermission(function = Function.TASK_CHECKLIST, operation = Operation.VIEW)
    public CareTaskClosure closure(Target target, long id) {
        return tasks.closure(id);
    }

    @RequiresPermission(function = Function.SHIFT_TASK_PLANNING, operation = Operation.VIEW)
    public CareShiftSources sources(Target target, long id) {
        var s = commands.shift(id);
        var staff = commands.staff();
        var rows = new ArrayList<CareShiftSources.ResidentSources>();
        var date = s.startsAt().atZone(BusinessTime.ZONE).toLocalDate();
        var lastDate = s.endsAt().minusNanos(1).atZone(BusinessTime.ZONE).toLocalDate();
        for (long resident : residents.byStatus(Set.of(
                ResidentStatus.IN_RESIDENCE,
                ResidentStatus.TEMPORARILY_ABSENT,
                ResidentStatus.HOSPITALIZED,
                ResidentStatus.OFF_SITE_ACTIVITY))) {
            var assigned = staff.assignments(resident, id);
            boolean inFloor = beds.openAt(resident, s.startsAt())
                    .map(b -> b.floorAreaId() == s.floorAreaId())
                    .orElse(false);
            if (!inFloor && assigned.isEmpty()) continue;
            if (!permissions
                    .hasPermission(
                            user,
                            new PermissionKey(Function.SHIFT_TASK_PLANNING, Operation.VIEW),
                            new Target("M04.TASK", "0", resident, s.floorAreaId()))
                    .allowed()) continue;
            var other = sources.stream()
                    .flatMap(source -> source.sources(resident, s.startsAt(), s.endsAt()).stream())
                    .toList();
            var planItems = date.datesUntil(lastDate.plusDays(1))
                    .flatMap(day -> plans.effectiveOn(resident, day).stream())
                    .flatMap(plan -> plan.items().stream())
                    .distinct()
                    .toList();
            rows.add(new CareShiftSources.ResidentSources(
                    resident, residents.status(resident).name(), assigned, planItems, other));
        }
        return new CareShiftSources(s, List.copyOf(rows));
    }
}
