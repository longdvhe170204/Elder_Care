package com.eldercare.backend.care.internal.session.service;

import static com.eldercare.backend.care.internal.activity.domain.ActivityKinds.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.internal.activity.domain.Activity;
import com.eldercare.backend.care.internal.session.domain.*;
import com.eldercare.backend.care.internal.session.domain.SessionTemplate.VersionFrom;
import com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T009 (FR-004, FR-006 → FR-013; research L-04): buổi do Trưởng tầng tạo bằng lệnh; hệ thống không tự tạo buổi
 * (Q-306). Điều kiện đăng ký sẵn theo {@code RegistrationEligibility} (FR-015, FR-016; US2).
 */
@Service
public class ActivitySessionService {
    public static final String ENTITY = "M04.ACTIVITY_SESSION";

    private final ActivitySessionStore store;
    private final SimpleRecordApi records;
    private final RegistrationEligibility eligibility;
    private final RegistrationNotices notices;
    private final CurrentUser user;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    public ActivitySessionService(
            ActivitySessionStore store,
            SimpleRecordApi records,
            RegistrationEligibility eligibility,
            RegistrationNotices notices,
            CurrentUser user,
            AuditLogApi audit,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.store = store;
        this.records = records;
        this.eligibility = eligibility;
        this.notices = notices;
        this.user = user;
        this.audit = audit;
        this.time = time;
        this.events = events;
    }

    public static Target target(Long floorAreaId) {
        return new Target(ENTITY, null, null, floorAreaId);
    }

    public Target target(long id) {
        var s = store.find(id).orElseThrow(() -> new com.eldercare.backend.common.error.NotFoundException());
        return new Target(ENTITY, Long.toString(id), null, s.floorAreaId());
    }

    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<ListedSession> list(Target target, LocalDate from, LocalDate to, Long floorAreaId) {
        var start = from == null ? time.today() : from;
        var end = to == null ? start.plusDays(6) : to;
        if (end.isBefore(start)) throw new InvalidValueException("Khoảng ngày không hợp lệ");
        return store.list(start, end, floorAreaId);
    }

    public record Detail(Session session, List<Registration> registrations) {}

    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Detail detail(Target target, long id) {
        var s = store.find(id).orElseThrow();
        return new Detail(s, store.activeRegistrations(id));
    }

    /** FR-006 → FR-008: tạo buổi còn thiếu trong khoảng, đăng ký sẵn người thường xuyên đạt điều kiện. */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public List<Long> createFromTemplate(Target target, long activityId, LocalDate from, LocalDate to) {
        requireFloorManager();
        if (from == null || to == null) throw new InvalidValueException("Cần khoảng ngày");
        if (from.isBefore(time.today())) throw new InvalidValueException("Ngày đầu không sớm hơn hôm nay");
        if (to.isBefore(from)) throw new InvalidValueException("Ngày cuối không trước ngày đầu");
        var activity = records.lockForUpdate(ACTIVITIES, activityId);
        requireActive(activity, from);
        var versions = versions(activityId);
        if (versions.isEmpty()) throw new InvalidValueException("Hoạt động chưa có mẫu lặp");
        var slots = SessionTemplate.slots(versions, from, to);
        var latest = versions.stream()
                .max(Comparator.comparing(VersionFrom::effectiveFrom))
                .orElseThrow()
                .version();
        if (slots.isEmpty() && latest.endDate() != null && from.isAfter(latest.endDate()))
            throw new BusinessException(
                    HttpStatus.CONFLICT, "AFTER_TEMPLATE_END_DATE", "Khoảng ngày sau ngày kết thúc của mẫu");
        var existing = store.starts(
                activityId,
                from.atStartOfDay(BusinessTime.ZONE).toInstant(),
                to.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant());
        var regulars = regulars(activityId);
        var created = new ArrayList<Long>();
        var skipped = new LinkedHashSet<Long>();
        var now = time.now();
        for (var slot : slots) {
            if (existing.contains(slot.startsAt())) continue;
            long id = store.insert(
                    sessionOf(activity, slot.startsAt(), slot.endsAt(), Session.TEMPLATE, null, null),
                    user.accountId(),
                    now);
            created.add(id);
            var session = store.lock(id);
            for (long resident : regulars) {
                if (!eligible(session, activity.content(), resident, now)) skipped.add(resident);
                else store.register(id, resident, Registration.REGULAR, null, now);
            }
        }
        if (!skipped.isEmpty())
            events.publishEvent(new RegularParticipantsNotRegistered(
                    activityId,
                    List.copyOf(skipped),
                    context(activity, "M04.ACTIVITY", activityId, "NOT_REGISTERED:" + now, now)));
        log(
                "M04.ACTIVITY",
                activityId,
                "SESSIONS_CREATED_FROM_TEMPLATE",
                Map.of("from", from.toString(), "to", to.toString(), "created", created.size()),
                null);
        return created;
    }

    /** FR-011: buổi lẻ cho hoạt động Hiệu lực, thời điểm tương lai. */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public long createSingle(
            Target target, long activityId, Instant startsAt, Instant endsAt, Long roomId, Long responsibleStaffId) {
        requireFloorManager();
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt))
            throw new InvalidValueException("Giờ kết thúc phải sau giờ bắt đầu");
        if (!startsAt.isAfter(time.now())) throw new InvalidValueException("Buổi lẻ phải ở tương lai");
        var activity = records.lockForUpdate(ACTIVITIES, activityId);
        requireActive(activity, LocalDate.ofInstant(startsAt, BusinessTime.ZONE));
        if (store.exists(activityId, startsAt))
            throw new BusinessException(HttpStatus.CONFLICT, "SESSION_ALREADY_EXISTS", "Đã có buổi cùng giờ bắt đầu");
        long id = store.insert(
                sessionOf(activity, startsAt, endsAt, Session.SINGLE, roomId, responsibleStaffId),
                user.accountId(),
                time.now());
        log(ENTITY, id, "SESSION_CREATED", Map.of("activityId", activityId, "startsAt", startsAt.toString()), null);
        return id;
    }

    /** FR-012: dời buổi chưa bắt đầu, có lý do; đăng ký không còn đạt điều kiện bị hủy; 005 nhận thời điểm mới. */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = true)
    @Transactional
    public void reschedule(
            Target target,
            long id,
            Reason reason,
            Instant startsAt,
            Instant endsAt,
            Long roomId,
            Long responsibleStaffId) {
        requireFloorManager();
        if (reason == null) throw new InvalidValueException("Dời buổi cần lý do");
        var before = store.lock(id);
        requireNotStarted(before);
        var start = startsAt == null ? before.startsAt() : startsAt;
        var end = endsAt == null ? before.endsAt() : endsAt;
        if (!end.isAfter(start)) throw new InvalidValueException("Giờ kết thúc phải sau giờ bắt đầu");
        if (!start.isAfter(time.now())) throw new InvalidValueException("Thời điểm mới phải ở tương lai");
        if (!start.equals(before.startsAt()) && store.exists(before.activityId(), start))
            throw new BusinessException(HttpStatus.CONFLICT, "SESSION_ALREADY_EXISTS", "Đã có buổi cùng giờ bắt đầu");
        store.reschedule(id, start, end, roomId, responsibleStaffId);
        var after = store.lock(id);
        var activity = records.require(ACTIVITIES, before.activityId()).content();
        var now = time.now();
        var cancelled = new ArrayList<Registration>();
        for (var r : store.activeRegistrations(id))
            eligibility
                    .tryCheck(after, activity, r.residentId(), now, false)
                    .ifPresent(result -> result.blockers().stream()
                            .filter(c -> !c.code().equals("CAPACITY_FULL"))
                            .findFirst()
                            .ifPresent(c -> {
                                store.cancelRegistration(r.id(), Registration.AUTO_SESSION_CHANGED, c.description());
                                cancelled.add(r);
                            }));
        notices.cancelled(after, cancelled, Registration.AUTO_SESSION_CHANGED, now);
        events.publishEvent(new ActivitySessionChanged(id));
        log(ENTITY, id, "SESSION_RESCHEDULED", Map.of("startsAt", start.toString(), "endsAt", end.toString()), reason);
    }

    /** FR-013: hủy buổi chưa có điểm danh, có lý do; mọi đăng ký Đã hủy; 005 hủy công việc "hoạt động". */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = true)
    @Transactional
    public void cancel(Target target, long id, Reason reason) {
        requireFloorManager();
        if (reason == null) throw new InvalidValueException("Hủy buổi cần lý do");
        var session = store.lock(id);
        if (!Session.SCHEDULED.equals(session.status()))
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Buổi không ở Đã lên lịch");
        cancelSession(session, Session.BY_STAFF, reason.value(), user.staffId().orElse(null));
        log(ENTITY, id, "SESSION_CANCELLED", Map.of("status", Session.CANCELLED), reason);
    }

    // Tác động kèm lệnh của hoạt động (ActivityService), cùng transaction.

    /** FR-004: ngừng hoạt động hủy buổi chưa điểm danh từ ngày ngừng. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void onDeactivated(long activityId, LocalDate from, String reason) {
        for (var s : store.scheduledFrom(
                activityId, from.atStartOfDay(BusinessTime.ZONE).toInstant()))
            if (!hasAttendance(s.id())) cancelSession(s, Session.BY_DEACTIVATION, reason, null);
    }

    /** FR-009: hủy buổi chưa diễn ra từ ngày áp dụng không khớp mẫu mới; giữ buổi cùng thời điểm; báo Trưởng tầng. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void onTemplateChanged(long activityId, LocalDate effectiveDate, String reason) {
        var now = time.now();
        var from = effectiveDate.atStartOfDay(BusinessTime.ZONE).toInstant();
        var candidates = store.scheduledFrom(activityId, from.isAfter(now) ? from : now).stream()
                .filter(s -> Session.TEMPLATE.equals(s.source()))
                .toList();
        if (candidates.isEmpty()) return;
        var last = LocalDate.ofInstant(candidates.getLast().startsAt(), BusinessTime.ZONE);
        var keep = new HashSet<Instant>();
        SessionTemplate.slots(versions(activityId), effectiveDate, last).forEach(slot -> keep.add(slot.startsAt()));
        var cancelled = new ArrayList<Long>();
        for (var s : candidates)
            if (!keep.contains(s.startsAt()) && !hasAttendance(s.id())) {
                cancelSession(s, Session.BY_TEMPLATE_CHANGE, reason, null);
                cancelled.add(s.id());
            }
        if (!cancelled.isEmpty()) {
            var activity = records.require(ACTIVITIES, activityId);
            events.publishEvent(new ActivitySessionsCancelledByTemplateChange(
                    activityId,
                    cancelled,
                    context(activity, "M04.ACTIVITY", activityId, "TEMPLATE:" + effectiveDate, now)));
        }
    }

    /** FR-010: thêm người thường xuyên đăng ký họ vào buổi đã tạo chưa diễn ra (theo điều kiện đăng ký). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void onRegularAdded(long activityId, long residentId) {
        var now = time.now();
        var activity = records.require(ACTIVITIES, activityId).content();
        for (var s : store.scheduledFrom(activityId, now))
            if (!store.registered(s.id(), residentId) && eligible(s, activity, residentId, now))
                store.register(s.id(), residentId, Registration.REGULAR, null, now);
    }

    /** FR-010: bỏ người thường xuyên hủy đăng ký sẵn ở buổi chưa diễn ra, giữ đăng ký thêm tay. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void onRegularRemoved(long activityId, long residentId, String reason) {
        for (var s : store.scheduledFrom(activityId, time.now()))
            for (var r : store.activeRegistrations(s.id()))
                if (r.residentId() == residentId && Registration.REGULAR.equals(r.source()))
                    store.cancelRegistration(r.id(), null, reason);
    }

    /** FR-004, FR-009, FR-013, FR-021: hủy buổi chưa có điểm danh kèm mọi đăng ký, báo người đã đăng ký. */
    void cancelSession(Session session, String by, String reason, Long staffId) {
        if (hasAttendance(session.id()))
            throw new BusinessException(HttpStatus.CONFLICT, "ATTENDANCE_ALREADY_RECORDED", "Buổi đã có điểm danh");
        store.cancel(session.id(), by, reason, staffId);
        String auto = switch (by) {
            case Session.BY_TEMPLATE_CHANGE -> Registration.AUTO_TEMPLATE_CHANGED;
            case Session.BY_ISOLATION -> Registration.AUTO_ISOLATION;
            default -> Registration.AUTO_SESSION_CANCELLED;
        };
        var registrations = store.activeRegistrations(session.id());
        for (var r : registrations) store.cancelRegistration(r.id(), auto, reason);
        notices.cancelled(session, registrations, auto, time.now());
        events.publishEvent(new ActivitySessionCancelled(session.id()));
    }

    /** Điều kiện đăng ký sẵn (FR-008, FR-010): có điều kiện chặn hoặc nguồn 007 chưa có thì không đăng ký. */
    private boolean eligible(Session session, Activity activity, long residentId, Instant now) {
        return eligibility
                .tryCheck(session, activity, residentId, now, false)
                .map(result -> result.blockers().isEmpty())
                .orElse(false);
    }

    private boolean hasAttendance(long sessionId) {
        return store.activeRegistrations(sessionId).stream().anyMatch(r -> Registration.ATTENDED.equals(r.status()));
    }

    private void requireNotStarted(Session s) {
        if (!Session.SCHEDULED.equals(s.status()))
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Buổi không ở Đã lên lịch");
        if (!s.startsAt().isAfter(time.now()))
            throw new BusinessException(HttpStatus.CONFLICT, "SESSION_ALREADY_STARTED", "Buổi đã bắt đầu");
    }

    private void requireActive(SimpleRecord<Activity> activity, LocalDate day) {
        var deactivatedFrom = activity.content().deactivatedFrom();
        if (!ACTIVE.equals(activity.status()) && (deactivatedFrom == null || !day.isBefore(deactivatedFrom)))
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Hoạt động đã ngừng");
    }

    private Session sessionOf(
            SimpleRecord<Activity> activity, Instant start, Instant end, String source, Long roomId, Long responsible) {
        var a = activity.content();
        var location = a.defaultLocation();
        return new Session(
                0,
                activity.id(),
                start,
                end,
                source,
                roomId != null ? roomId : location == null ? null : location.roomId(),
                roomId != null || location == null ? null : location.areaId(),
                a.floorAreaId(),
                responsible != null ? responsible : a.defaultResponsibleStaffId(),
                Session.SCHEDULED,
                false,
                null,
                null,
                0);
    }

    private List<VersionFrom> versions(long activityId) {
        return records.listByParent(TEMPLATE_VERSIONS, new EntityRef("M04.ACTIVITY", activityId)).stream()
                .filter(r -> ACTIVE.equals(r.status()))
                .map(r -> new VersionFrom(r.effectiveFrom(), r.content()))
                .toList();
    }

    private List<Long> regulars(long activityId) {
        return records.listByParent(REGULAR_PARTICIPANTS, new EntityRef("M04.ACTIVITY", activityId)).stream()
                .filter(r -> ACTIVE.equals(r.status()) && r.residentId() != null)
                .map(SimpleRecord::residentId)
                .toList();
    }

    private void requireFloorManager() {
        if (!user.roles().contains(Role.FLOOR_MANAGER))
            throw new PermissionDeniedException("Chỉ Trưởng tầng tạo, dời, hủy buổi");
    }

    static List<RecipientGroup> floorManagerOf(Activity a) {
        return a.floorAreaId() != null
                ? List.of(new RecipientGroup.FloorManager(a.floorAreaId(), new Substitution.Default()))
                : List.of(new RecipientGroup.Staff(a.createdByStaffId(), "ACTIVITY_CREATOR"));
    }

    private static CareEventContext context(
            SimpleRecord<Activity> activity, String type, long id, String milestone, Instant at) {
        return new CareEventContext(
                type,
                id,
                milestone,
                at,
                Optional.ofNullable(activity.content().floorAreaId()),
                floorManagerOf(activity.content()));
    }

    private void log(String entity, long id, String action, Map<String, Object> after, Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, reason != null))
                .entity(entity, id)
                .before(Map.of())
                .after(after)
                .reason(reason)
                .build());
    }
}
