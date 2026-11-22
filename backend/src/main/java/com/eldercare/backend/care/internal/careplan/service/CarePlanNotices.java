package com.eldercare.backend.care.internal.careplan.service;

import com.eldercare.backend.administration.account.api.ApproverApi;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.internal.careplan.domain.CarePlan;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** Mỗi sự kiện chỉ mang một người duyệt, không vượt giới hạn 4000 ký tự của outbox. */
@Service
public class CarePlanNotices {
    private final ApproverApi approvers;
    private final ApplicationEventPublisher events;

    public CarePlanNotices(ApproverApi approvers, ApplicationEventPublisher events) {
        this.approvers = approvers;
        this.events = events;
    }

    public List<Long> approvers(CarePlan plan) {
        return approvers.approvers(
                new PermissionKey(Function.CARE_PLAN, Operation.APPROVE),
                CarePlanService.residentTarget(plan.residentId(), plan.id()));
    }

    public void submitted(CarePlan plan, Instant now) {
        publish(plan, now, approvers(plan), false);
    }

    public void overdue(CarePlan plan, Instant now, List<Long> recipients) {
        publish(plan, now, recipients, true);
    }

    private void publish(CarePlan plan, Instant now, List<Long> recipients, boolean overdue) {
        for (long accountId : recipients.stream().distinct().toList()) {
            var context = new CareEventContext(
                    "M04.CARE_PLAN",
                    plan.id(),
                    (overdue ? "APPROVAL_OVERDUE:" : "SUBMITTED:") + plan.version() + ":" + accountId,
                    now,
                    Optional.empty(),
                    List.of(new RecipientGroup.Approvers("M04.CARE_PLAN", List.of(accountId))));
            if (overdue) events.publishEvent(new CarePlanApprovalOverdue(plan.id(), plan.residentId(), context));
            else events.publishEvent(new CarePlanSubmitted(plan.id(), plan.residentId(), context));
        }
    }
}
