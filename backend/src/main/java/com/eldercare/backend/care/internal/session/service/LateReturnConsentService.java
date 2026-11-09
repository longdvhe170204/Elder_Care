package com.eldercare.backend.care.internal.session.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.admission.api.spi.RepresentativePort;
import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.events.LateReturnConsentNotice;
import com.eldercare.backend.care.internal.attendance.service.AttendanceSources;
import com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork;
import com.eldercare.backend.care.internal.session.domain.*;
import com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.care.internal.trip.service.TripLifecycle;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** FR-016a, Q-176, Q-401: đồng ý theo từng buổi, giờ về theo ngày do 005 giữ. */
@Service
public class LateReturnConsentService implements CareMinuteWork {
    private static final SharedKind<Consent> CONSENTS = SharedKind.of("M04.LATE_RETURN_CONSENT", Consent.class);
    private static final String ACTIVE = "ACTIVE";
    private static final String USED = "USED";
    private static final String WITHDRAWN = "WITHDRAWN";
    private static final String EXPIRED = "EXPIRED";
    private static final String WITHDRAWAL = "CONSENT_WITHDRAWN";

    private final SimpleRecordApi records;
    private final ActivitySessionStore sessions;
    private final ActivityTripStore trips;
    private final AttendanceSources sources;
    private final DayCareAttendanceApi attendance;
    private final CareLockStore locks;
    private final ObjectProvider<RepresentativePort> representatives;
    private final TripLifecycle tripLifecycle;
    private final RegistrationNotices notices;
    private final FileApi files;
    private final AuditLogApi audit;
    private final CurrentUser user;
    private final BusinessTime time;

    public LateReturnConsentService(
            SimpleRecordApi records,
            ActivitySessionStore sessions,
            ActivityTripStore trips,
            AttendanceSources sources,
            DayCareAttendanceApi attendance,
            CareLockStore locks,
            ObjectProvider<RepresentativePort> representatives,
            TripLifecycle tripLifecycle,
            RegistrationNotices notices,
            FileApi files,
            AuditLogApi audit,
            CurrentUser user,
            BusinessTime time) {
        this.records = records;
        this.sessions = sessions;
        this.trips = trips;
        this.sources = sources;
        this.attendance = attendance;
        this.locks = locks;
        this.representatives = representatives;
        this.tripLifecycle = tripLifecycle;
        this.notices = notices;
        this.files = files;
        this.audit = audit;
        this.user = user;
        this.time = time;
    }

    record Consent(
            long sessionId,
            long representativeId,
            String method,
            Long signedFileId,
            LocalTime agreedReturnTime,
            Long withdrawnBy,
            Instant withdrawnAt) {}

    private record SessionTime(Instant startsAt, Instant endsAt) {
        LocalDate date() {
            return LocalDate.ofInstant(startsAt, BusinessTime.ZONE);
        }

        boolean sameDay() {
            return date().equals(LocalDate.ofInstant(endsAt, BusinessTime.ZONE));
        }
    }

    private record ReturnTime(Session session, Registration registration, long representativeId, LocalTime time) {
        long registrationId() {
            return registration.id();
        }
    }

    public Target target(long sessionId, long residentId) {
        var session = sessions.find(sessionId).orElseThrow(NotFoundException::new);
        return new Target(null, null, residentId, session.floorAreaId());
    }

    public Target target(long id) {
        var row = records.require(CONSENTS, id);
        return target(row.content().sessionId(), row.residentId());
    }

    @RequiresPermission(function = Function.LATE_RETURN_CONSENT, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void create(
            Target target, long sessionId, long residentId, Long signedBy, Long signedFileId, boolean portal) {
        requireChannel(portal);
        if (!portal && (signedBy == null || signedBy <= 0 || signedFileId == null || signedFileId <= 0))
            throw new InvalidValueException("Cần người đại diện và tệp bản ký");
        long representative = portal ? user.familyMemberId().orElseThrow() : signedBy;
        requireRepresentative(representative, residentId);
        var session = sessions.lock(sessionId);
        requireBeforeStart(session);
        var times = times(session);
        var schedule = registrationSchedule(session, residentId, false);
        if (schedule == null
                || schedule.arrival() == null
                || schedule.departure() == null
                || schedule.sessionStart().isBefore(schedule.arrival())
                || !schedule.sessionEnd().isAfter(schedule.departure()))
            throw conflict("OUTSIDE_DAY_CARE_SCHEDULE", "Buổi phải bắt đầu trong ngày bán trú và cần về muộn");
        long id = records.create(
                        CONSENTS,
                        new NewSimpleRecord<>(
                                residentId,
                                new EntityRef("M04.ACTIVITY_SESSION", sessionId),
                                ACTIVE,
                                new Consent(
                                        sessionId,
                                        representative,
                                        portal ? "PORTAL" : "SIGNED",
                                        signedFileId,
                                        schedule.sessionEnd(),
                                        null,
                                        null),
                                times.date(),
                                null,
                                key(sessionId, residentId),
                                user.accountId(),
                                false))
                .id();
        if (signedFileId != null) files.attach(signedFileId, CONSENTS.code(), id);
        audit(id, residentId, "LATE_RETURN_CONSENT_RECORDED", null, ACTIVE, null);
    }

    @RequiresPermission(function = Function.LATE_RETURN_CONSENT, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void withdraw(Target target, long id, Reason reason, boolean portal) {
        requireChannel(portal);
        if (!portal && reason == null) throw new ReasonRequiredException();
        var before = records.require(CONSENTS, id);
        var session = sessions.lock(before.content().sessionId());
        locks.lock(List.of(before.residentId()));
        var row = records.lockForUpdate(CONSENTS, id);
        if (!ACTIVE.equals(row.status())) throw conflict("STATE_CHANGED", "Đồng ý đã đổi trạng thái");
        requireBeforeStart(session);
        if (portal) requireRepresentative(user.familyMemberId().orElseThrow(), row.residentId());
        var content = row.content();
        records.change(
                CONSENTS,
                id,
                row.version(),
                new RecordChange<>(
                        WITHDRAWN,
                        new Consent(
                                content.sessionId(),
                                content.representativeId(),
                                content.method(),
                                content.signedFileId(),
                                content.agreedReturnTime(),
                                user.accountId(),
                                time.now()),
                        null,
                        null,
                        false));
        sessions.activeRegistration(session.id(), row.residentId()).ifPresent(registration -> {
            sessions.cancelRegistration(registration.id(), WITHDRAWAL, "Người đại diện rút đồng ý về muộn");
            tripLifecycle.registrationChanged(session.id(), row.residentId(), true);
            updateReturnTime(row.residentId(), row.effectiveFrom(), registration.id());
            notices.lateReturn(
                    session,
                    registration,
                    LateReturnConsentNotice.WITHDRAWN,
                    row.effectiveFrom(),
                    null,
                    null,
                    time.now());
        });
        audit(id, row.residentId(), "LATE_RETURN_CONSENT_WITHDRAWN", ACTIVE, WITHDRAWN, reason);
    }

    RegistrationConditions.DayCare registrationSchedule(Session session, long residentId, boolean hasConsent) {
        var times = times(session);
        if (!sources.dayCare(residentId, times.date())) return null;
        var day = sources.day(residentId, times.date());
        return new RegistrationConditions.DayCare(
                LocalTime.ofInstant(times.startsAt(), BusinessTime.ZONE),
                LocalTime.ofInstant(times.endsAt(), BusinessTime.ZONE),
                day.arrival(),
                times.sameDay() ? day.departure() : null,
                hasConsent);
    }

    boolean hasActiveConsent(Session session, long residentId) {
        var times = times(session);
        return records.findByUniqueKey(CONSENTS, key(session.id(), residentId))
                .filter(row -> ACTIVE.equals(row.status())
                        && times.date().equals(row.effectiveFrom())
                        && times.sameDay()
                        && !LocalTime.ofInstant(times.endsAt(), BusinessTime.ZONE)
                                .isAfter(row.content().agreedReturnTime()))
                .isPresent();
    }

    void registrationSaved(Session session, long residentId, long registrationId) {
        if (hasActiveConsent(session, residentId))
            updateReturnTime(residentId, times(session).date(), registrationId);
    }

    void registrationCancelled(Session session, Registration registration) {
        expire(session.id(), registration.residentId(), registration.id());
    }

    /** FR-016a, FR-043: chuyến gia hạn thì giờ về của người có đồng ý dời theo giờ về dự kiến mới của chuyến. */
    public void tripExtended(long sessionId) {
        for (var consent : records.listByParent(CONSENTS, new EntityRef("M04.ACTIVITY_SESSION", sessionId)))
            if (ACTIVE.equals(consent.status()) || USED.equals(consent.status()))
                sessions.findActiveRegistration(sessionId, consent.residentId())
                        .ifPresent(r -> updateReturnTime(consent.residentId(), consent.effectiveFrom(), r.id()));
    }

    void sessionCancelled(long sessionId) {
        for (var consent : records.listByParent(CONSENTS, new EntityRef("M04.ACTIVITY_SESSION", sessionId))) {
            if (ACTIVE.equals(consent.status())) expire(sessionId, consent.residentId(), null);
        }
    }

    private void expire(long sessionId, long residentId, Long cancelledRegistrationId) {
        records.findByUniqueKey(CONSENTS, key(sessionId, residentId))
                .filter(row -> ACTIVE.equals(row.status()))
                .ifPresent(candidate -> {
                    locks.lock(List.of(residentId));
                    var row = records.lockForUpdate(CONSENTS, candidate.id());
                    if (!ACTIVE.equals(row.status())) return;
                    records.change(
                            CONSENTS,
                            row.id(),
                            row.version(),
                            new RecordChange<>(EXPIRED, row.content(), null, null, false));
                    if (cancelledRegistrationId != null)
                        updateReturnTime(residentId, row.effectiveFrom(), cancelledRegistrationId);
                    audit(row.id(), residentId, "LATE_RETURN_CONSENT_EXPIRED", ACTIVE, EXPIRED, null);
                });
    }

    private void updateReturnTime(long residentId, LocalDate date, long changedRegistrationId) {
        locks.lock(List.of(residentId));
        var scheduled = attendance
                .onDate(residentId, date)
                .flatMap(view -> view.expectedReturn())
                .orElseGet(() -> sources.day(residentId, date).departure());
        var latest = records.listByResident(CONSENTS, residentId, Set.of(ACTIVE, USED)).stream()
                .filter(row -> date.equals(row.effectiveFrom()))
                .flatMap(row -> sessions.findActiveRegistration(row.content().sessionId(), residentId).stream()
                        .flatMap(r -> sessions.find(r.sessionId()).stream()
                                .map(s -> new ReturnTime(
                                        s,
                                        r,
                                        row.content().representativeId(),
                                        LocalTime.ofInstant(times(s).endsAt(), BusinessTime.ZONE)))))
                .filter(candidate -> scheduled != null && candidate.time().isAfter(scheduled))
                .max(Comparator.comparing(ReturnTime::time).thenComparingLong(ReturnTime::registrationId));
        if (latest.isPresent()) {
            var value = latest.get();
            attendance.changeReturnTime(residentId, date, value.time(), value.registrationId());
            notices.lateReturn(
                    value.session(),
                    value.registration(),
                    LateReturnConsentNotice.RETURN_TIME_CHANGED,
                    date,
                    value.time(),
                    value.representativeId(),
                    time.now());
        } else {
            attendance.restoreReturnTime(residentId, date, changedRegistrationId);
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int run(Instant now) {
        int count = 0;
        for (long id : sessions.dueLateReturnConsents(now)) {
            var candidate = records.require(CONSENTS, id);
            var session = sessions.lock(candidate.content().sessionId());
            var row = records.lockForUpdate(CONSENTS, id);
            if (!ACTIVE.equals(row.status())
                    || times(session).startsAt().isAfter(now)
                    || !sessions.registered(session.id(), row.residentId())) continue;
            records.change(
                    CONSENTS,
                    id,
                    row.version(),
                    new RecordChange<>(USED, row.content(), null, key(session.id(), row.residentId()), false));
            audit(id, row.residentId(), "LATE_RETURN_CONSENT_USED", ACTIVE, USED, null);
            count++;
        }
        return count;
    }

    private SessionTime times(Session session) {
        if (session.tripStatus() == null) return new SessionTime(session.startsAt(), session.endsAt());
        var plan = trips.find(session.id()).orElseThrow(NotFoundException::new).plan();
        return new SessionTime(plan.plannedDepartureAt(), plan.plannedReturnAt());
    }

    private void requireBeforeStart(Session session) {
        if (!Session.SCHEDULED.equals(session.status())
                || (session.tripStatus() != null && !"SCHEDULED".equals(session.tripStatus()))
                || !times(session).startsAt().isAfter(time.now()))
            throw conflict("SESSION_ALREADY_STARTED", "Buổi không còn ở thời điểm trước khi bắt đầu");
    }

    private void requireChannel(boolean portal) {
        if (portal ? user.familyMemberId().isEmpty() : !user.roles().contains(Role.OFFICE_STAFF))
            throw new PermissionDeniedException("Kênh ghi đồng ý không phù hợp với tài khoản");
    }

    private void requireRepresentative(long representative, long residentId) {
        var source = representatives.getIfAvailable();
        if (source == null) throw AttendanceSources.unavailable("Chưa có nguồn người đại diện");
        if (!source.isActiveRepresentative(representative, residentId, time.today()))
            throw new PermissionDeniedException("Chỉ người đại diện hiệu lực được đồng ý về muộn");
    }

    private void audit(long id, long residentId, String action, String before, String after, Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, reason != null))
                .entity(CONSENTS.code(), id)
                .resident(residentId)
                .before(before == null ? Map.of() : Map.of("status", before))
                .after(Map.of("status", after))
                .reason(reason)
                .build());
    }

    private static String key(long sessionId, long residentId) {
        return sessionId + ":" + residentId;
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
