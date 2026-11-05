package com.eldercare.backend.care.internal.session.service;

import static com.eldercare.backend.care.internal.activity.domain.ActivityKinds.ACTIVITIES;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.care.internal.session.domain.Registration;
import com.eldercare.backend.care.internal.session.domain.Session;
import com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * 014 T011 (FR-015 → FR-019, BR-M04-15; research L-05): đăng ký do Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc lập (quyền
 * {@code ACTIVITY_OUTING}, FR-017). Số lượng xét dưới khóa dòng buổi nên hai đăng ký chỗ cuối không cùng lọt.
 */
@Service
public class RegistrationService {
    public static final String ENTITY = "M04.ACTIVITY_REGISTRATION";

    private final ActivitySessionStore store;
    private final SimpleRecordApi records;
    private final RegistrationEligibility eligibility;
    private final CurrentUser user;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final JsonMapper json;
    private final com.eldercare.backend.care.internal.trip.service.TripLifecycle trips;
    private final ResidentSessionReminders reminders;

    public RegistrationService(
            ActivitySessionStore store,
            SimpleRecordApi records,
            RegistrationEligibility eligibility,
            CurrentUser user,
            AuditLogApi audit,
            BusinessTime time,
            JsonMapper json,
            com.eldercare.backend.care.internal.trip.service.TripLifecycle trips,
            ResidentSessionReminders reminders) {
        this.store = store;
        this.records = records;
        this.eligibility = eligibility;
        this.user = user;
        this.audit = audit;
        this.time = time;
        this.json = json;
        this.trips = trips;
        this.reminders = reminders;
    }

    public Target target(long sessionId, Long residentId) {
        var session = store.find(sessionId).orElseThrow(NotFoundException::new);
        return new Target(ENTITY, null, residentId, session.floorAreaId());
    }

    public Target targetOfRegistration(long registrationId) {
        var r = store.lockRegistration(registrationId);
        var session = store.find(r.sessionId()).orElseThrow(NotFoundException::new);
        return new Target(ENTITY, Long.toString(registrationId), r.residentId(), session.floorAreaId());
    }

    /** FR-015 → FR-018: điều kiện chặn trả 409 kèm danh sách; cảnh báo cần xác nhận, lưu danh sách đã hiển thị. */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public long register(Target target, long sessionId, long residentId, boolean acknowledgeWarnings) {
        var session = store.lock(sessionId);
        var activity = records.require(ACTIVITIES, session.activityId()).content();
        var now = time.now();
        var result = eligibility.check(session, activity, residentId, now, false);
        if (!result.blockers().isEmpty())
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    result.blockers().getFirst().code(),
                    result.blockers().getFirst().description(),
                    Map.of("unmetConditions", result.blockers()));
        if (!result.warnings().isEmpty() && !acknowledgeWarnings)
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "WARNING_ACKNOWLEDGEMENT_REQUIRED",
                    "Cần xác nhận cảnh báo trước khi đăng ký",
                    Map.of("unmetConditions", result.warnings()));
        if (store.registered(sessionId, residentId))
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Người cao tuổi đã đăng ký buổi này");
        long id;
        try {
            id = store.register(
                    sessionId,
                    residentId,
                    Registration.MANUAL,
                    user.staffId().orElse(null),
                    now,
                    result.warnings().isEmpty() ? null : json.writeValueAsString(result.warnings()));
        } catch (DuplicateKeyException e) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Người cao tuổi đã đăng ký buổi này");
        }
        log(id, "REGISTRATION_CREATED", Map.of("sessionId", sessionId, "residentId", residentId), null);
        trips.registrationChanged(sessionId, residentId, false);
        // FR-071 (Q-366): đăng ký trong ngày thì nhắc chính người cao tuổi ngay.
        reminders.registered(session, id, residentId);
        return id;
    }

    /** FR-017: hủy trước giờ bắt đầu, có lý do. */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional
    public void cancel(Target target, long registrationId, Reason reason) {
        long sessionId = store.findRegistration(registrationId)
                .orElseThrow(NotFoundException::new)
                .sessionId();
        // Cùng thứ tự khóa với đăng ký và rời viện (buổi rồi đăng ký) để hai lệnh không chờ vòng nhau.
        var session = store.lock(sessionId);
        var r = store.lockRegistration(registrationId);
        if (!Registration.REGISTERED.equals(r.status()) || !Session.SCHEDULED.equals(session.status()))
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Đăng ký không còn ở Đã đăng ký");
        if (session.tripStatus() != null && !"SCHEDULED".equals(session.tripStatus()))
            throw new BusinessException(HttpStatus.CONFLICT, "SESSION_ALREADY_STARTED", "Chuyến đã có người rời viện");
        if (session.tripStatus() == null && !session.startsAt().isAfter(time.now()))
            throw new BusinessException(HttpStatus.CONFLICT, "SESSION_ALREADY_STARTED", "Buổi đã bắt đầu");
        store.cancelRegistration(registrationId, null, reason.value());
        trips.registrationChanged(r.sessionId(), r.residentId(), true);
        log(registrationId, "REGISTRATION_CANCELLED", Map.of("status", Registration.CANCELLED), reason);
    }

    private void log(long id, String action, Map<String, Object> after, Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, reason != null))
                .entity(ENTITY, id)
                .before(Map.of())
                .after(after)
                .reason(reason)
                .build());
    }
}
