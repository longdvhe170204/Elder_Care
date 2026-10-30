package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.common.notification.*;
import java.time.*;
import java.util.*;

/** FR-019: hai mức cho hai nhóm, không gửi mức Trung bình tới Trưởng tầng. */
public record DayCarePresentAtDayEnd(long residentId, LocalDate date, CareEventContext context) implements CareNotice {
    @Override
    public NotificationLevel level() {
        return NotificationLevel.MEDIUM;
    }

    @Override
    public Optional<Long> notificationResidentId() {
        return Optional.of(residentId);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        var leads = context.recipients().stream()
                .filter(r -> r instanceof RecipientGroup.ShiftLead)
                .toList();
        var managers = context.recipients().stream()
                .filter(r -> r instanceof RecipientGroup.FloorManager)
                .toList();
        var result = new ArrayList<NotificationRequest>();
        if (!leads.isEmpty())
            result.add(
                    CareEventNotifications.create(this, NotificationLevel.MEDIUM, leads, eventKey() + ":SHIFT_LEAD"));
        if (!managers.isEmpty())
            result.add(CareEventNotifications.create(
                    this, NotificationLevel.LOW, managers, eventKey() + ":FLOOR_MANAGER"));
        return List.copyOf(result);
    }
}
