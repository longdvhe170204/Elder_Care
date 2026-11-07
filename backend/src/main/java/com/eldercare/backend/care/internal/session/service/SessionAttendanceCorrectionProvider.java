package com.eldercare.backend.care.internal.session.service;

import static com.eldercare.backend.care.internal.activity.domain.ActivityKinds.ACTIVITIES;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.billing.api.events.CostSourceChanged;
import com.eldercare.backend.care.internal.session.domain.SessionKinds;
import com.eldercare.backend.care.internal.session.domain.SessionKinds.AttendanceResult;
import com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T014 (FR-031; module-interfaces 014 mục 1.1 {@code DinhChinhDuocProvider} {@code M04.DIEM_DANH_BUOI}): điểm danh
 * nhóm 3, gắn tầng của buổi; người ghi nhận là Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc (FR-027). Tác động tính phí
 * theo nguồn đang hiệu lực của đăng ký ({@code billed_source_id}), nên đính chính nhiều lần vẫn đúng.
 */
@Component
public class SessionAttendanceCorrectionProvider implements CorrectableRecordProvider {
    private final RecordEventApi recordEvents;
    private final SimpleRecordApi records;
    private final ActivitySessionStore store;
    private final SessionAttendanceService attendance;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    public SessionAttendanceCorrectionProvider(
            RecordEventApi recordEvents,
            SimpleRecordApi records,
            ActivitySessionStore store,
            SessionAttendanceService attendance,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.recordEvents = recordEvents;
        this.records = records;
        this.store = store;
        this.attendance = attendance;
        this.time = time;
        this.events = events;
    }

    @Override
    public String recordType() {
        return SessionKinds.ATTENDANCE.code();
    }

    private AttendanceResult content(long id) {
        return recordEvents.require(SessionKinds.ATTENDANCE, id).content();
    }

    @Override
    public boolean isConfirmed(long id) {
        try {
            return recordEvents.require(SessionKinds.ATTENDANCE, id).confirmedAt() != null;
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public long originalRecorder(long id) {
        var actor = recordEvents.require(SessionKinds.ATTENDANCE, id).actorId();
        return actor == null ? 0 : actor;
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.ofNullable(
                recordEvents.require(SessionKinds.ATTENDANCE, id).floorAreaId());
    }

    @Override
    public Optional<Long> residentId(long id) {
        return Optional.of(content(id).residentId());
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return !Collections.disjoint(user.roles(), Set.of(Role.FLOOR_MANAGER, Role.NURSING_STAFF, Role.CAREGIVER));
    }

    @Override
    public void validateContent(Object corrected) {
        SessionAttendanceService.validate(item(corrected));
    }

    @Override
    public Object originalContent(long id) {
        var c = content(id);
        var map = new LinkedHashMap<String, Object>();
        map.put("result", c.result());
        if (c.absenceReason() != null) map.put("absenceReason", c.absenceReason());
        if (c.participationLevel() != null) map.put("participationLevel", c.participationLevel());
        if (c.communicationLevel() != null) map.put("communicationLevel", c.communicationLevel());
        if (c.conditionAfter() != null) map.put("conditionAfter", c.conditionAfter());
        if (c.note() != null) map.put("note", c.note());
        return map;
    }

    /** FR-031: Có mặt thành Vắng hoặc Hủy ghi nhận thì hủy nguồn; thêm lượt Có mặt thì nguồn mới (mã đính chính). */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void afterCorrection(CorrectionView correction) {
        var original = content(correction.originalRecordId());
        var session = store.find(original.sessionId()).orElseThrow();
        var activity = records.require(ACTIVITIES, session.activityId()).content();
        if (!activity.charged()) return;
        boolean present = correction.type() == CorrectionType.AMEND_CONTENT
                && SessionKinds.PRESENT.equals(
                        item(correction.correctedContent()).result());
        var billed = store.billedSource(original.registrationId());
        if (billed.isPresent() && !present) {
            events.publishEvent(new CostSourceChanged(
                    SessionAttendanceService.BILLING_SOURCE,
                    billed.get(),
                    SessionAttendanceService.BILLING_UNIT,
                    Optional.empty(),
                    true,
                    "CORRECTION:" + correction.id(),
                    SessionAttendanceService.BILLING_SOURCE + ":" + billed.get()));
            store.billedSource(original.registrationId(), null);
        } else if (billed.isEmpty() && present) {
            String source = "C" + correction.id();
            attendance.bill(source, original.residentId(), activity, session, time.now());
            store.billedSource(original.registrationId(), source);
        }
    }

    private static SessionAttendanceService.Item item(Object value) {
        if (!(value instanceof Map<?, ?> map)) throw new InvalidValueException("Nội dung đính chính phải là đối tượng");
        return new SessionAttendanceService.Item(
                0L,
                false,
                text(map, "result"),
                text(map, "absenceReason"),
                text(map, "participationLevel"),
                text(map, "communicationLevel"),
                text(map, "conditionAfter"),
                text(map, "note"));
    }

    private static String text(Map<?, ?> map, String key) {
        var v = map.get(key);
        return v == null ? null : v.toString();
    }
}
