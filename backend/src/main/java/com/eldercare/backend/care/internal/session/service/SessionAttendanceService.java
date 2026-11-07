package com.eldercare.backend.care.internal.session.service;

import static com.eldercare.backend.care.internal.activity.domain.ActivityKinds.ACTIVITIES;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.care.api.events.ActivityCostSourceBilled;
import com.eldercare.backend.care.api.events.ActivitySessionEnded;
import com.eldercare.backend.care.internal.activity.domain.Activity;
import com.eldercare.backend.care.internal.session.domain.Registration;
import com.eldercare.backend.care.internal.session.domain.Session;
import com.eldercare.backend.care.internal.session.domain.SessionKinds;
import com.eldercare.backend.care.internal.session.domain.SessionKinds.AttendanceResult;
import com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T014 (FR-027 → FR-032, BR-M04-18; research L-06): điểm danh buổi trong viện là {@code record_event}
 * {@code M04.SESSION_ATTENDANCE} nhóm 3 (sửa bằng đính chính). Lượt Có mặt ở hoạt động có thu phí là một nguồn tính phí
 * {@code ACTIVITY_ATTENDANCE} cho 010; không tạo khoản (Q-306).
 */
@Service
public class SessionAttendanceService {
    public static final String BILLING_SOURCE = "ACTIVITY_ATTENDANCE";
    public static final String BILLING_UNIT = "OCCURRENCE";

    static final Set<String> ABSENCE_REASONS = Set.of("REFUSED", "HEALTH", "AWAY", "NO_SHOW", "OTHER");
    static final Set<String> PARTICIPATION = Set.of("ACTIVE", "NORMAL", "PASSIVE", "LEFT_EARLY");
    static final Set<String> COMMUNICATION = Set.of("PROACTIVE", "WHEN_ASKED", "MINIMAL");
    /** FR-028: người thêm lúc điểm danh chỉ xét FR-015 (a) → (d) và (g). */
    static final Set<String> WALK_IN_BLOCKERS =
            Set.of("CAPACITY_FULL", "ISOLATION_ACTIVE", "RESIDENT_STATUS_INVALID", "INFECTION_SUSPECTED");

    public record Item(
            Long residentId,
            Boolean addedAtAttendance,
            String result,
            String absenceReason,
            String participationLevel,
            String communicationLevel,
            String conditionAfter,
            String note) {}

    private final ActivitySessionStore store;
    private final SimpleRecordApi records;
    private final RecordEventApi recordEvents;
    private final RegistrationEligibility eligibility;
    private final ParameterApi parameters;
    private final CurrentUser user;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    public SessionAttendanceService(
            ActivitySessionStore store,
            SimpleRecordApi records,
            RecordEventApi recordEvents,
            RegistrationEligibility eligibility,
            ParameterApi parameters,
            CurrentUser user,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.store = store;
        this.records = records;
        this.recordEvents = recordEvents;
        this.eligibility = eligibility;
        this.parameters = parameters;
        this.user = user;
        this.time = time;
        this.events = events;
    }

    /** FR-027 → FR-030: ghi một hoặc nhiều kết quả; mỗi đăng ký một kết quả, sai thì đính chính (FR-031). */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void record(Target target, long sessionId, List<Item> items, Instant deviceAt) {
        apply(sessionId, items, deviceAt);
    }

    /** Dùng chung cho lệnh trực tuyến và bản ghi ngoại tuyến đã qua kiểm quyền của 002 (Q-01). */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Long> apply(long sessionId, List<Item> items, Instant deviceAt) {
        if (items == null || items.isEmpty()) throw new InvalidValueException("Cần ít nhất một kết quả");
        items.forEach(SessionAttendanceService::validate);
        var session = store.lock(sessionId);
        requireInstituteSession(session);
        if (!Session.SCHEDULED.equals(session.status()))
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Buổi không ở Đã lên lịch");
        var now = time.now();
        var recordedAt = deviceAt == null ? now : deviceAt;
        if (recordedAt.isBefore(session.startsAt()))
            throw new BusinessException(HttpStatus.CONFLICT, "NOT_YET_STARTED", "Chưa tới giờ bắt đầu của buổi");
        var activity = records.require(ACTIVITIES, session.activityId()).content();
        boolean late = recordedAt.isAfter(
                session.endsAt().plus(parameters.read(ParameterCode.CFG_M04_06).asDuration()));
        var ids = new ArrayList<Long>();
        for (var item : items) ids.add(one(session, activity, item, late, deviceAt, now));
        return ids;
    }

    private long one(Session session, Activity activity, Item item, boolean late, Instant deviceAt, Instant now) {
        long resident = item.residentId();
        var registration = store.activeRegistration(session.id(), resident).orElse(null);
        if (registration == null) {
            if (!Boolean.TRUE.equals(item.addedAtAttendance()))
                throw new BusinessException(
                        HttpStatus.CONFLICT, "STATE_CHANGED", "Người cao tuổi chưa đăng ký buổi này");
            var blocker = eligibility.check(session, activity, resident, now, false).blockers().stream()
                    .filter(c -> WALK_IN_BLOCKERS.contains(c.code()))
                    .findFirst();
            if (blocker.isPresent())
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        blocker.get().code(),
                        blocker.get().description(),
                        Map.of("unmetConditions", List.of(blocker.get())));
            long id = store.register(
                    session.id(),
                    resident,
                    Registration.ADDED_AT_ATTENDANCE,
                    user.staffId().orElse(null),
                    now);
            registration = store.lockRegistration(id);
        } else if (Registration.ATTENDED.equals(registration.status()))
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "ATTENDANCE_ALREADY_RECORDED",
                    "Đăng ký đã có kết quả điểm danh, sửa bằng đính chính");
        var content = new AttendanceResult(
                registration.id(),
                session.id(),
                resident,
                item.result(),
                SessionKinds.ABSENT.equals(item.result()) ? item.absenceReason() : null,
                SessionKinds.PRESENT.equals(item.result()) ? item.participationLevel() : null,
                SessionKinds.PRESENT.equals(item.result()) ? item.communicationLevel() : null,
                item.conditionAfter(),
                item.note(),
                late,
                deviceAt);
        var event = recordEvents.add(
                SessionKinds.ATTENDANCE,
                new EntityRef("M01.RESIDENT", resident),
                new NewRecordEvent<>(
                        content,
                        null,
                        user.accountId(),
                        false,
                        now,
                        deviceAt,
                        now,
                        session.floorAreaId(),
                        SessionKinds.key(registration.id())));
        store.markAttended(registration.id());
        if (SessionKinds.PRESENT.equals(item.result()) && activity.charged()) {
            String source = Long.toString(event.id());
            bill(source, resident, activity, session, now);
            store.billedSource(registration.id(), source);
        }
        return event.id();
    }

    /** FR-032: một lượt, dịch vụ của hoạt động, ngày của buổi. */
    void bill(String source, long resident, Activity activity, Session session, Instant now) {
        events.publishEvent(new ActivityCostSourceBilled(
                BILLING_SOURCE,
                source,
                BILLING_UNIT,
                resident,
                Optional.ofNullable(activity.serviceId()),
                BigDecimal.ONE,
                LocalDate.ofInstant(session.startsAt(), BusinessTime.ZONE),
                now,
                BILLING_SOURCE + ":" + source));
    }

    /** FR-029: Hoàn tất khi mọi đăng ký còn hiệu lực đã có kết quả. */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void complete(Target target, long sessionId) {
        var session = store.lock(sessionId);
        requireInstituteSession(session);
        if (!Session.SCHEDULED.equals(session.status()))
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Buổi không ở Đã lên lịch");
        if (session.startsAt().isAfter(time.now()))
            throw new BusinessException(HttpStatus.CONFLICT, "NOT_YET_STARTED", "Chưa tới giờ bắt đầu của buổi");
        if (store.activeRegistrations(sessionId).stream().anyMatch(r -> !Registration.ATTENDED.equals(r.status())))
            throw new BusinessException(
                    HttpStatus.CONFLICT, "REGISTRATIONS_WITHOUT_RESULT", "Còn đăng ký chưa có kết quả điểm danh");
        store.close(sessionId, Session.ATTENDANCE_TAKEN, false);
        events.publishEvent(new ActivitySessionEnded(sessionId));
    }

    private static void requireInstituteSession(Session session) {
        if (session.tripStatus() != null)
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "STATE_CHANGED",
                    "Chuyến đi phải điểm danh rời viện, không dùng điểm danh buổi trong viện");
    }

    static void validate(Item item) {
        if (item.residentId() == null) throw new InvalidValueException("Thiếu người cao tuổi");
        if (SessionKinds.PRESENT.equals(item.result())) {
            if (item.participationLevel() == null || !PARTICIPATION.contains(item.participationLevel()))
                throw new InvalidValueException("Có mặt cần mức độ tham gia");
            if (item.communicationLevel() != null && !COMMUNICATION.contains(item.communicationLevel()))
                throw new InvalidValueException("Mức giao tiếp không hợp lệ");
        } else if (SessionKinds.ABSENT.equals(item.result())) {
            if (item.absenceReason() == null || !ABSENCE_REASONS.contains(item.absenceReason()))
                throw new InvalidValueException("Vắng cần lý do vắng");
        } else throw new InvalidValueException("Kết quả phải là PRESENT hoặc ABSENT");
    }
}
