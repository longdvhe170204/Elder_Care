package com.eldercare.backend.family.internal.visit.service;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.admission.api.events.DayCareAbsenceNoticeRecorded;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.events.RelationshipChange;
import com.eldercare.backend.family.api.events.RelationshipChanged;
import com.eldercare.backend.family.api.events.VisitCancelledNotice;
import com.eldercare.backend.family.api.events.VisitCheckOutReminder;
import com.eldercare.backend.family.api.events.VisitIsolationNotice;
import com.eldercare.backend.family.internal.visit.domain.Visit;
import com.eldercare.backend.family.internal.visit.domain.Visit.Canceller;
import com.eldercare.backend.family.internal.visit.domain.Visit.Status;
import com.eldercare.backend.family.internal.visit.domain.VisitRegistrationCheck;
import com.eldercare.backend.incident.api.events.IsolationCreated;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tự động của lượt thăm (research F-08; FR-034, FR-036, FR-037; Q-306: không lập bản ghi nghiệp vụ, chỉ chuyển trạng thái
 * và báo). Người cao tuổi chuyển Tạm vắng, Điều trị tại bệnh viện, trạng thái cuối, hoặc bán trú báo vắng: lượt Đã duyệt
 * trùng thời gian thành Hủy, người đăng ký nhận Nhẹ. Khoanh vùng (007): lượt Đã duyệt của người cao tuổi trong vùng chưa
 * hết khung thành Hủy, người đăng ký nhận Trung bình; lượt Đã vào giữ nguyên; Hành chính, Trưởng tầng nhận danh sách. Kết
 * thúc quan hệ: lượt chưa đến của người đó thành Hủy. Việc theo phút: hết khung chưa vào thành Không đến; Đã vào quá giờ
 * kết thúc cộng CFG-M10-07 nhắc Hành chính một lần.
 */
@Component
class VisitAutomation {

    private static final Set<ResidentStatus> AWAY = Set.of(
            ResidentStatus.TEMPORARILY_ABSENT,
            ResidentStatus.HOSPITALIZED,
            ResidentStatus.RESIDENCY_ENDED,
            ResidentStatus.DECEASED,
            ResidentStatus.ADMISSION_CANCELLED);
    private static final String RELATIONSHIP_ENDED = "RELATIONSHIP_ENDED";

    private final SimpleRecordApi records;
    private final TechnicalLocks locks;
    private final VisitService visits;
    private final VisitRules rules;
    private final VisitPresence presence;
    private final ResidentLocationApi locations;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;

    VisitAutomation(
            SimpleRecordApi records,
            TechnicalLocks locks,
            VisitService visits,
            VisitRules rules,
            VisitPresence presence,
            ResidentLocationApi locations,
            BusinessTime time,
            ApplicationEventPublisher events,
            PlatformTransactionManager transactions) {
        this.records = records;
        this.locks = locks;
        this.visits = visits;
        this.rules = rules;
        this.presence = presence;
        this.locations = locations;
        this.time = time;
        this.events = events;
        this.tx = new TransactionTemplate(transactions);
    }

    @EventListener
    @Transactional
    public void residentStatusChanged(ResidentStatusChanged event) {
        if (AWAY.contains(event.to())) {
            cancelOf(
                    event.residentId(),
                    v -> v.content().slotEnd().isAfter(event.at()),
                    VisitRegistrationCheck.Reason.RESIDENT_NOT_PRESENT.name(),
                    event.at());
        }
    }

    @EventListener
    @Transactional
    public void dayCareAbsence(DayCareAbsenceNoticeRecorded event) {
        cancelOf(
                event.residentId(),
                v -> v.content().date().equals(event.date()),
                VisitRegistrationCheck.Reason.RESIDENT_NOT_PRESENT.name(),
                time.now());
    }

    @EventListener
    @Transactional
    public void relationshipEnded(RelationshipChanged event) {
        if (event.change() == RelationshipChange.ENDED) {
            cancelOf(
                    event.residentId(),
                    v -> v.content().familyMemberId() == event.familyMemberId()
                            && v.content().slotEnd().isAfter(event.at()),
                    RELATIONSHIP_ENDED,
                    event.at());
        }
    }

    // Tham số kiểu Object: IsolationCreated mang yêu cầu thông báo, chỉ 009 được nghe kiểu đó (M13ArchTest FR-001)
    @EventListener(IsolationCreated.class)
    @Transactional
    public void isolationCreated(Object payload) {
        IsolationCreated event = (IsolationCreated) payload;
        Instant now = time.now();
        List<Long> cancelled = new ArrayList<>();
        List<Long> checkedIn = new ArrayList<>();
        Set<Long> affected = new HashSet<>();
        for (SimpleRecord<Visit> visit : records.listByStatus(Visit.KIND, VisitService.HOLDING)) {
            if (!visit.content().slotEnd().isAfter(now) || !presence.inZone(visit.residentId(), event.zoneId(), now)) {
                continue;
            }
            affected.add(visit.residentId());
            if (visit.status().equals(Status.CHECKED_IN.name())) {
                checkedIn.add(visit.id());
            } else if (cancel(visit.id(), VisitRegistrationCheck.Reason.ISOLATION_ACTIVE.name(), now)) {
                cancelled.add(visit.id());
            }
        }
        if (cancelled.isEmpty() && checkedIn.isEmpty()) {
            return;
        }
        Set<Long> floors = new HashSet<>();
        affected.forEach(r -> locations.floorOn(r, time.today()).ifPresent(floors::add));
        events.publishEvent(new VisitIsolationNotice(event.zoneId(), cancelled, checkedIn, floors, now));
    }

    /** FR-034: lượt Đã duyệt hết khung chưa vào thành Không đến (trả chỗ). */
    int markNoShows(Instant at) {
        int changed = 0;
        for (SimpleRecord<Visit> visit : records.listByStatus(Visit.KIND, Set.of(Status.APPROVED.name()))) {
            if (!visit.content().slotEnd().isAfter(at)) {
                Boolean done = tx.execute(s -> {
                    SimpleRecord<Visit> current = records.lockForUpdate(Visit.KIND, visit.id());
                    if (!current.status().equals(Status.APPROVED.name())) {
                        return false;
                    }
                    visits.close(current, Status.NO_SHOW, current.content());
                    return true;
                });
                changed += Boolean.TRUE.equals(done) ? 1 : 0;
            }
        }
        return changed;
    }

    /** FR-034: Đã vào quá giờ kết thúc khung cộng CFG-M10-07 chưa ghi ra, nhắc Hành chính một lần. */
    int remindCheckOut(Instant at) {
        Duration after = rules.checkOutReminderAfter();
        int reminded = 0;
        for (SimpleRecord<Visit> visit : records.listByStatus(Visit.KIND, Set.of(Status.CHECKED_IN.name()))) {
            if (visit.content().checkOutRemindedAt() == null
                    && !visit.content().slotEnd().plus(after).isAfter(at)) {
                Boolean done = tx.execute(s -> {
                    SimpleRecord<Visit> current = records.lockForUpdate(Visit.KIND, visit.id());
                    if (!current.status().equals(Status.CHECKED_IN.name())
                            || current.content().checkOutRemindedAt() != null) {
                        return false;
                    }
                    records.change(
                            Visit.KIND,
                            current.id(),
                            current.version(),
                            new RecordChange<>(
                                    current.status(),
                                    current.content().reminded(at),
                                    current.effectiveTo(),
                                    null,
                                    false));
                    events.publishEvent(new VisitCheckOutReminder(current.id(), current.residentId(), at));
                    return true;
                });
                reminded += Boolean.TRUE.equals(done) ? 1 : 0;
            }
        }
        return reminded;
    }

    private void cancelOf(long residentId, Predicate<SimpleRecord<Visit>> matches, String cause, Instant at) {
        for (SimpleRecord<Visit> visit :
                records.listByResident(Visit.KIND, residentId, Set.of(Status.APPROVED.name()))) {
            if (matches.test(visit)) {
                cancel(visit.id(), cause, at);
            }
        }
    }

    /** Hủy do hệ thống, trả chỗ; báo người đăng ký trừ khi kết thúc quan hệ. */
    private boolean cancel(long visitId, String cause, Instant at) {
        SimpleRecord<Visit> current = records.lockForUpdate(Visit.KIND, visitId);
        if (!current.status().equals(Status.APPROVED.name())) {
            return false;
        }
        Visit visit = current.content();
        locks.lock(visit.slot().lockKey(visit.date()));
        String reason = RELATIONSHIP_ENDED.equals(cause)
                ? "Quan hệ người thân đã kết thúc"
                : VisitRegistrationCheck.Reason.valueOf(cause).description();
        // Hệ thống hủy: không có phiên người dùng để ghi nhật ký thao tác; dòng ghi người hủy, mã lý do
        visits.close(current, Status.CANCELLED, visit.cancelled(Canceller.SYSTEM, cause, reason, null));
        if (!RELATIONSHIP_ENDED.equals(cause)) {
            events.publishEvent(new VisitCancelledNotice(
                    visitId,
                    current.residentId(),
                    visit.familyMemberId(),
                    visit.date(),
                    visit.startTime(),
                    visit.endTime(),
                    cause,
                    at));
        }
        return true;
    }
}
