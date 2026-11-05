package com.eldercare.backend.care.internal.session.service;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.ActivitySessionDayEnded;
import com.eldercare.backend.care.api.events.ActivitySessionEnded;
import com.eldercare.backend.care.internal.activity.service.ActivityScheduling;
import com.eldercare.backend.care.internal.scheduling.service.CareDailyWork;
import com.eldercare.backend.care.internal.session.domain.Registration;
import com.eldercare.backend.care.internal.session.domain.Session;
import com.eldercare.backend.care.internal.session.domain.SessionKinds;
import com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 014 T009 (FR-014a, Q-169; research L-15): hết ngày của buổi, buổi trong viện còn Đã lên lịch nhận "Không ghi nhận" cho
 * đăng ký chưa có kết quả và sang Đã điểm danh (dấu "điểm danh không đủ") hoặc Không điểm danh; báo người phụ trách buổi,
 * Trưởng tầng. Chỉ chuyển trạng thái, không tạo buổi (Q-306).
 */
@Component
public class SessionDayEnd implements CareDailyWork {
    private final ActivitySessionStore store;
    private final RecordEventApi recordEvents;
    private final ApplicationEventPublisher events;

    public SessionDayEnd(ActivitySessionStore store, RecordEventApi recordEvents, ApplicationEventPublisher events) {
        this.store = store;
        this.recordEvents = recordEvents;
        this.events = events;
    }

    @Override
    public String name() {
        return ActivityScheduling.PREFIX + "SESSION_DAY_END";
    }

    @Override
    public List<LocalDate> dueDates(LocalDate today) {
        return ActivityScheduling.dueDates(today, store.oldestScheduledBefore(today));
    }

    @Override
    public int run(LocalDate date, Instant now) {
        int count = 0;
        for (var session : store.scheduledOn(date)) {
            var registrations = store.activeRegistrations(session.id());
            boolean attended = registrations.stream().anyMatch(r -> Registration.ATTENDED.equals(r.status()));
            for (var r : registrations)
                if (Registration.REGISTERED.equals(r.status())) {
                    store.markAttended(r.id());
                    recordEvents.add(
                            SessionKinds.ATTENDANCE,
                            new EntityRef("M01.RESIDENT", r.residentId()),
                            new NewRecordEvent<>(
                                    new SessionKinds.AttendanceResult(
                                            r.id(), session.id(), SessionKinds.NOT_RECORDED, false),
                                    null,
                                    null,
                                    true,
                                    now,
                                    null,
                                    now,
                                    session.floorAreaId(),
                                    SessionKinds.key(r.id())));
                }
            store.close(session.id(), attended ? Session.ATTENDANCE_TAKEN : Session.NO_ATTENDANCE, attended);
            events.publishEvent(new ActivitySessionEnded(session.id()));
            events.publishEvent(new ActivitySessionDayEnded(session.id(), !attended, context(session, date, now)));
            count++;
        }
        return count;
    }

    private static CareEventContext context(Session session, LocalDate date, Instant now) {
        var recipients = new ArrayList<RecipientGroup>();
        if (session.responsibleStaffId() != null)
            recipients.add(new RecipientGroup.Staff(session.responsibleStaffId(), "SESSION_RESPONSIBLE"));
        if (session.floorAreaId() != null)
            recipients.add(new RecipientGroup.FloorManager(session.floorAreaId(), new Substitution.Default()));
        return new CareEventContext(
                ActivitySessionService.ENTITY,
                session.id(),
                "DAY_END:" + date,
                now,
                Optional.ofNullable(session.floorAreaId()),
                recipients);
    }
}
