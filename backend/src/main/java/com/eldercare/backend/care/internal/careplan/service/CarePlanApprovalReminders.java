package com.eldercare.backend.care.internal.careplan.service;

import com.eldercare.backend.administration.approval.api.OverdueReminder;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.parameter.api.ParameterCode;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.CarePlanManagerEscalation;
import com.eldercare.backend.care.internal.careplan.domain.CarePlan;
import com.eldercare.backend.care.internal.careplan.persistence.CarePlanStore;
import com.eldercare.backend.care.internal.scheduling.service.CareDailyWork;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import java.time.*;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** T014/C-12: nhắc qua 000 theo giờ liên tục; không tự hủy hay sửa nội dung. */
@Component
public class CarePlanApprovalReminders implements CareDailyWork {
    private final CarePlanStore store;
    private final CareLockStore locks;
    private final OverdueReminder reminder;
    private final CarePlanNotices notices;
    private final ApplicationEventPublisher events;
    private final AuditLogApi audit;

    public CarePlanApprovalReminders(
            CarePlanStore store,
            CareLockStore locks,
            OverdueReminder reminder,
            CarePlanNotices notices,
            ApplicationEventPublisher events,
            AuditLogApi audit) {
        this.store = store;
        this.locks = locks;
        this.reminder = reminder;
        this.notices = notices;
        this.events = events;
        this.audit = audit;
    }

    @Override
    public String name() {
        return "CARE_PLAN_APPROVAL_REMINDER";
    }

    @Override
    public List<LocalDate> dueDates(LocalDate today) {
        return store.pendingApproval().isEmpty() ? List.of() : List.of(today);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int run(LocalDate date, Instant now) {
        var candidates = store.pendingApproval();
        locks.lock(candidates.stream().map(CarePlan::residentId).toList());
        int count = 0;
        for (var candidate : candidates) {
            var plan = store.locked(candidate.id());
            if (plan.status() != CarePlanStatus.PENDING_APPROVAL) continue;
            var recipients = notices.approvers(plan);
            var decision = reminder.check(
                    plan.submittedAt(),
                    plan.remindedAt(),
                    plan.escalatedAt(),
                    ParameterCode.CFG_M15_05,
                    ParameterCode.CFG_M15_06,
                    !recipients.isEmpty());
            if (!decision.remind() && decision.escalation() == null) continue;
            if (decision.remind()) notices.overdue(plan, now, recipients);
            if (decision.escalation() != null) {
                var context = new CareEventContext(
                        "M04.CARE_PLAN",
                        plan.id(),
                        "APPROVAL_ESCALATION:" + plan.version(),
                        now,
                        Optional.empty(),
                        List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)));
                events.publishEvent(new CarePlanManagerEscalation(plan.id(), plan.residentId(), context));
            }
            store.reminder(
                    plan.id(),
                    decision.remind() ? now : plan.remindedAt(),
                    decision.escalation() != null ? now : plan.escalatedAt());
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("CARE_PLAN_APPROVAL_REMINDER", false))
                    .entity("M04.CARE_PLAN", plan.id())
                    .after(Map.of(
                            "reminded",
                            decision.remind(),
                            "escalation",
                            decision.escalation() == null
                                    ? "NONE"
                                    : decision.escalation().name()))
                    .basis(new Basis("CFG-M15-05/06", null, plan.id(), MDC.get(SchedulerRuns.MDC_KEY)))
                    .build());
            count++;
        }
        return count;
    }
}
