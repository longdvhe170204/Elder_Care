package com.eldercare.backend.care.internal.careplan.service;

import com.eldercare.backend.administration.account.api.ApproverApi;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.admission.api.events.AddendumApplied;
import com.eldercare.backend.care.api.CarePlanReviewApi;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.CarePlanReviewNeeded;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.events.AssessmentChangedCareLevelOrNeeds;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-M04-20: báo xem xét kế hoạch khi nguồn đã thay đổi. Nguồn 001, 004 qua sự kiện; nguồn 007 (ngã, chăm sóc giảm nhẹ) qua
 * lệnh {@link CarePlanReviewApi} 007 gọi (duyệt 08/11, 09/11/2026).
 */
@Component
class CarePlanReviewListener implements CarePlanReviewApi {
    private static final String CARE_PLAN = "M04.CARE_PLAN";
    private static final PermissionKey APPROVE = new PermissionKey(Function.CARE_PLAN, Operation.APPROVE);
    private static final String REVIEW_MARKER = "M04.CARE_PLAN_REVIEW";
    private static final String INCIDENT = "M05.INCIDENT";
    private static final String PALLIATIVE = "M05.PALLIATIVE_CARE_CHOICE";

    private final ApproverApi approvers;
    private final BedAllocationApi allocations;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final ReminderMarkers markers;

    CarePlanReviewListener(
            ApproverApi approvers,
            BedAllocationApi allocations,
            BusinessTime time,
            ApplicationEventPublisher events,
            ReminderMarkers markers) {
        this.approvers = approvers;
        this.allocations = allocations;
        this.time = time;
        this.events = events;
        this.markers = markers;
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

    /** 007 gọi trong lệnh ghi sự cố ngã; dấu theo sự cố chống báo lặp. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void fallIncidentRecorded(long residentId, long incidentId) {
        if (!markers.mark(REVIEW_MARKER, INCIDENT, incidentId, "FALL", null)) return;
        review(residentId, INCIDENT, incidentId, Long.toString(incidentId), "", time.now());
    }

    /**
     * 007 gọi trong lệnh ghi lựa chọn chăm sóc giảm nhẹ. Nguồn không có id số nên {@code sourceId} là người cao tuổi, khóa
     * của lần chọn ({@code eventKey}) vào mốc để hai lần chọn khác nhau cho hai bộ thông báo khác khóa; dấu theo băm của khóa
     * (cột {@code marker} 80 ký tự).
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void palliativeCareChoiceRecorded(long residentId, String eventKey, Instant occurredAt) {
        Objects.requireNonNull(eventKey);
        Objects.requireNonNull(occurredAt);
        if (!markers.mark(
                REVIEW_MARKER,
                "M01.RESIDENT",
                residentId,
                "PALLIATIVE:" + sha256(eventKey),
                Map.of("eventKey", eventKey))) return;
        review(residentId, PALLIATIVE, residentId, eventKey, eventKey + ":", occurredAt);
    }

    private void review(long residentId, String source, long sourceId) {
        review(residentId, source, sourceId, Long.toString(sourceId), "", time.now());
    }

    private void review(
            long residentId, String source, long sourceId, String reference, String milestonePrefix, Instant now) {
        var floor = allocations.openAt(residentId, now).map(BedAllocationView::floorAreaId);
        var target = new Target(CARE_PLAN, null, residentId, floor.orElse(null));
        var accounts = approvers.approvers(APPROVE, target);
        publish(
                residentId,
                source,
                sourceId,
                reference,
                now,
                floor,
                milestonePrefix + "ASSIGNED_NURSE",
                new RecipientGroup.AssignedNurse(residentId, new Substitution.Default()));
        // Mỗi event duyệt mang một tài khoản để payload outbox có kích thước hữu hạn.
        for (long accountId : accounts.stream().distinct().toList()) {
            publish(
                    residentId,
                    source,
                    sourceId,
                    reference,
                    now,
                    floor,
                    milestonePrefix + "APPROVER:" + accountId,
                    new RecipientGroup.Approvers(CARE_PLAN, List.of(accountId)));
        }
    }

    private void publish(
            long residentId,
            String source,
            long sourceId,
            String reference,
            Instant now,
            Optional<Long> floor,
            String milestone,
            RecipientGroup recipient) {
        var context = new CareEventContext(source, sourceId, milestone, now, floor, List.of(recipient));
        events.publishEvent(new CarePlanReviewNeeded(residentId, source, reference, context));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK thiếu SHA-256", e);
        }
    }
}
