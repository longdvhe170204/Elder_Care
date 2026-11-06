package com.eldercare.backend.care.internal.session.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.ActivityAttendanceReminder;
import com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork;
import com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T014 (FR-030; research L-15): tới kết thúc + CFG-M04-06 mà buổi chưa Hoàn tất điểm danh thì nhắc người phụ trách
 * buổi và Trưởng tầng một lần ({@code attendance_reminded_at}). Bước theo phút trong khung {@code CARE_MINUTE}.
 */
@Component
public class SessionAttendanceReminder implements CareMinuteWork {
    private final ActivitySessionStore store;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;

    public SessionAttendanceReminder(
            ActivitySessionStore store, ParameterApi parameters, ApplicationEventPublisher events) {
        this.store = store;
        this.parameters = parameters;
        this.events = events;
    }

    @Override
    @Transactional
    public int run(Instant now) {
        var due = store.dueForAttendanceReminder(
                now.minus(parameters.read(ParameterCode.CFG_M04_06).asDuration()));
        for (var s : due) {
            store.markAttendanceReminded(s.id(), now);
            var recipients = new ArrayList<RecipientGroup>();
            if (s.responsibleStaffId() != null)
                recipients.add(new RecipientGroup.Staff(s.responsibleStaffId(), "SESSION_RESPONSIBLE"));
            if (s.floorAreaId() != null)
                recipients.add(new RecipientGroup.FloorManager(s.floorAreaId(), new Substitution.Default()));
            events.publishEvent(new ActivityAttendanceReminder(
                    s.id(),
                    new CareEventContext(
                            ActivitySessionService.ENTITY,
                            s.id(),
                            "ATTENDANCE_REMINDER",
                            now,
                            Optional.ofNullable(s.floorAreaId()),
                            recipients)));
        }
        return due.size();
    }
}
