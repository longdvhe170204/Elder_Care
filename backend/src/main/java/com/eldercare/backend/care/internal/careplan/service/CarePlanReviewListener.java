package com.eldercare.backend.care.internal.careplan.service;

import com.eldercare.backend.administration.account.api.ApproverApi;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.admission.api.events.AddendumApplied;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.CarePlanReviewNeeded;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.events.AssessmentChangedCareLevelOrNeeds;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** BR-M04-20: báo xem xét kế hoạch khi nguồn đã thay đổi. */
@Component
class CarePlanReviewListener {
    private static final String CARE_PLAN = "M04.CARE_PLAN";
    private static final PermissionKey APPROVE = new PermissionKey(Function.CARE_PLAN, Operation.APPROVE);

    private final ApproverApi approvers;
    private final BedAllocationApi allocations;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    CarePlanReviewListener(
            ApproverApi approvers, BedAllocationApi allocations, BusinessTime time, ApplicationEventPublisher events) {
        this.approvers = approvers;
        this.allocations = allocations;
        this.time = time;
        this.events = events;
    }

    @EventListener
    public void onAssessment(AssessmentChangedCareLevelOrNeeds event) {
        review(event.residentId(), "M01.ASSESSMENT", event.assessmentId());
    }

    @EventListener
    public void onAddendum(AddendumApplied event) {
        if (event.changeTypes().contains("CARE_LEVEL")) {
            review(event.residentId(), "M02.ADDENDUM", event.addendumId());
        }
    }

    private void review(long residentId, String source, long sourceId) {
        Instant now = time.now();
        var floor = allocations.openAt(residentId, now).map(BedAllocationView::floorAreaId);
        var target = new Target(CARE_PLAN, null, residentId, floor.orElse(null));
        var accounts = approvers.approvers(APPROVE, target);
        publish(
                residentId,
                source,
                sourceId,
                now,
                floor,
                "ASSIGNED_NURSE",
                new RecipientGroup.AssignedNurse(residentId, new Substitution.Default()));
        // Mỗi event duyệt mang một tài khoản để payload outbox có kích thước hữu hạn.
        for (long accountId : accounts.stream().distinct().toList()) {
            publish(
                    residentId,
                    source,
                    sourceId,
                    now,
                    floor,
                    "APPROVER:" + accountId,
                    new RecipientGroup.Approvers(CARE_PLAN, List.of(accountId)));
        }
    }

    private void publish(
            long residentId,
            String source,
            long sourceId,
            Instant now,
            Optional<Long> floor,
            String milestone,
            RecipientGroup recipient) {
        var context = new CareEventContext(source, sourceId, milestone, now, floor, List.of(recipient));
        events.publishEvent(new CarePlanReviewNeeded(residentId, source, Long.toString(sourceId), context));
    }
}
