package com.eldercare.backend.care.internal.status.service;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.api.events.AlertClosureRequested;
import com.eldercare.backend.care.internal.careplan.persistence.CarePlanStore;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.domain.CareTaskTransitions;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareAbsenceTasks;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * T057 (contracts mục 1.1, research C-08): tác động của 005 trong transaction lệnh trạng thái của 001; chạy sau 004 (@Order
 * 1), 003 (@Order 20); không có điều kiện chặn.
 */
@Service
@Order(30)
public class CareStatusParticipant implements StatusTransitionParticipant {
    private static final Set<StatusCommand> DEPARTURE = EnumSet.of(
            StatusCommand.GRANT_TEMPORARY_ABSENCE, StatusCommand.TRANSFER_OUT, StatusCommand.ROLL_CALL_DEPARTURE);
    private static final Set<StatusCommand> RETURN =
            EnumSet.of(StatusCommand.RECORD_RETURN, StatusCommand.ROLL_CALL_RETURN);
    private static final Set<StatusCommand> FINAL =
            EnumSet.of(StatusCommand.END_RESIDENCY, StatusCommand.RECORD_DEATH, StatusCommand.CANCEL_ADMISSION);
    private static final String FINAL_RULE = "FR_010_FINAL_STATE";

    private final CareAbsenceTasks absence;
    private final CareTaskStore tasks;
    private final CareTaskService commands;
    private final CarePlanStore plans;
    private final CareLockStore locks;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public CareStatusParticipant(
            CareAbsenceTasks absence,
            CareTaskStore tasks,
            CareTaskService commands,
            CarePlanStore plans,
            CareLockStore locks,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.absence = absence;
        this.tasks = tasks;
        this.commands = commands;
        this.plans = plans;
        this.locks = locks;
        this.audit = audit;
        this.events = events;
        this.time = time;
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        var all = EnumSet.noneOf(StatusCommand.class);
        all.addAll(DEPARTURE);
        all.addAll(RETURN);
        all.addAll(FINAL);
        all.add(StatusCommand.COMPLETE_ADMISSION);
        all.add(StatusCommand.CANCEL_DEPARTURE_RECORD);
        return all;
    }

    @Override
    public List<Condition> checkConditions(CommandContext context) {
        return List.of();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyEffects(TransitionResult result) {
        long resident = result.residentId();
        var command = result.command();
        if (DEPARTURE.contains(command)) absence.departed(resident, result.at());
        else if (RETURN.contains(command))
            absence.planningNeeded(resident, "RETURN", result.at(), "RETURN:" + result.historyId());
        else if (command == StatusCommand.COMPLETE_ADMISSION)
            absence.planningNeeded(
                    resident, "ADMISSION_COMPLETED", result.at(), "ADMISSION_COMPLETED:" + result.historyId());
        else if (command == StatusCommand.CANCEL_DEPARTURE_RECORD)
            absence.recalculate(resident, Optional.empty(), Optional.empty());
        else if (FINAL.contains(command)) finalStatus(result);
    }

    /** FR-010, bảng FR-003 hai dòng cuối. */
    private void finalStatus(TransitionResult result) {
        long resident = result.residentId();
        locks.lock(List.of(resident));
        var reason = new Reason("trạng thái cuối: " + result.to());
        for (var candidate : tasks.openOfResident(resident)) {
            var before = tasks.get(candidate.id(), true);
            var next = CareTaskTransitions.next(before.status(), "RESIDENT_FINAL", before.importance());
            if (next.isEmpty()) continue;
            var after = tasks.transition(before, next.get(), time.now(), reason.value(), FINAL_RULE);
            commands.record(before, after, "CLOSED_ON_FINAL_STATE", reason, true);
            if (before.status() != TaskStatus.NOT_YET_DUE)
                events.publishEvent(new AlertClosureRequested(
                        before.id(),
                        "FINAL_STATE",
                        new CareEventContext(
                                "M04.TASK",
                                before.id(),
                                "FINAL_STATE:" + result.historyId(),
                                time.now(),
                                Optional.of(before.floorAreaId()),
                                List.of())));
        }
        var on = result.at().atZone(BusinessTime.ZONE).toLocalDate();
        for (var plan : plans.list(resident, true)) {
            plans.endForFinalStatus(plan, on);
            var after = plans.require(plan.id());
            if (after.status() == plan.status()) continue;
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("CARE_PLAN_ENDED_ON_FINAL_STATE", false))
                    .entity("M04.CARE_PLAN", plan.id())
                    .before(Map.of("status", plan.status()))
                    .after(Map.of("status", after.status(), "endReason", "FINAL_STATUS"))
                    .build());
        }
    }
}
