package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.ShiftWithoutTasks;
import com.eldercare.backend.care.api.spi.ShiftStaffPort;
import com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T024: chỉ nhắc ca còn đang diễn ra, marker và sự kiện cùng transaction. */
@Service
public class CareShiftReminders implements CareMinuteWork {
    private final ObjectProvider<ShiftStaffPort> staffing;
    private final CareTaskStore tasks;
    private final ReminderMarkers markers;
    private final ApplicationEventPublisher events;

    public CareShiftReminders(
            ObjectProvider<ShiftStaffPort> staffing,
            CareTaskStore tasks,
            ReminderMarkers markers,
            ApplicationEventPublisher events) {
        this.staffing = staffing;
        this.tasks = tasks;
        this.markers = markers;
        this.events = events;
    }

    @Override
    @Transactional
    public int run(Instant now) {
        var source = staffing.getIfAvailable();
        if (source == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "CARE_SOURCE_UNAVAILABLE",
                    "Chưa có nguồn mọi ca đang diễn ra của 008");
        int count = 0;
        for (var shift : source.ongoingShifts(now)) {
            if (!shift.published()
                    || now.isBefore(shift.startsAt())
                    || !now.isBefore(shift.endsAt())
                    || !tasks.inShift(shift.id()).isEmpty()) continue;
            if (!markers.mark(
                    "M04.SHIFT_WITHOUT_TASKS",
                    "M09.SHIFT",
                    shift.id(),
                    "START",
                    Map.of("startsAt", shift.startsAt().toString()))) continue;
            events.publishEvent(new ShiftWithoutTasks(
                    shift.id(),
                    shift.floorAreaId(),
                    new CareEventContext(
                            "M09.SHIFT",
                            shift.id(),
                            "SHIFT_WITHOUT_TASKS",
                            now,
                            Optional.of(shift.floorAreaId()),
                            List.of(
                                    new RecipientGroup.ShiftLead(shift.floorAreaId(), new Substitution.Default()),
                                    new RecipientGroup.FloorManager(
                                            shift.floorAreaId(), new Substitution.Default())))));
            count++;
        }
        return count;
    }
}
