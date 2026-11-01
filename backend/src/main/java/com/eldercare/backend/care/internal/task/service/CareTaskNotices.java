package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.notification.*;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** BR-M04-26: lập muộn có cùng nhắc ban đầu như chuyển Quá hạn; marker cùng transaction. */
@Service
public class CareTaskNotices {
    private final ReminderMarkers markers;
    private final ApplicationEventPublisher events;

    public CareTaskNotices(ReminderMarkers markers, ApplicationEventPublisher events) {
        this.markers = markers;
        this.events = events;
    }

    public void overdue(CareTask task, Instant at) {
        if (task.importance() == Importance.MANDATORY && task.residentId() == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "CARE_SOURCE_UNAVAILABLE",
                    "Hợp đồng cảnh báo chưa hỗ trợ công việc gắn vị trí không có người cao tuổi");
        if (!markers.mark("M04.TASK_OVERDUE", "M04.TASK", task.id(), "INITIAL", Map.of("at", at.toString()))) return;
        List<RecipientGroup> performer = task.performerId() == null
                ? List.of(new RecipientGroup.OnShiftStaff(
                        task.floorAreaId(),
                        Set.of(task.performerRole()),
                        task.data().certificates(),
                        new Substitution.Default()))
                : List.of(new RecipientGroup.Staff(task.performerId(), "CARE_TASK_PERFORMER"));
        events.publishEvent(new OverdueTaskReminder(task.id(), context(task, at, "REMINDER", performer)));
        var leads = List.<RecipientGroup>of(
                new RecipientGroup.FloorManager(task.floorAreaId(), new Substitution.Default()),
                new RecipientGroup.ShiftLead(task.floorAreaId(), new Substitution.Default()));
        if (task.importance() == Importance.IMPORTANT)
            events.publishEvent(
                    new OverdueTaskNeedsHandling(task.id(), task.importance(), context(task, at, "IMPORTANT", leads)));
        if (task.importance() == Importance.MANDATORY)
            events.publishEvent(new AlertRequested(
                    "OVERDUE_CARE_TASK",
                    NotificationLevel.MEDIUM,
                    task.residentId(),
                    "M04.TASK:" + task.id(),
                    task.basisType(),
                    task.basisId() == null ? task.id() : task.basisId(),
                    context(task, at, "MANDATORY", leads)));
    }

    private CareEventContext context(CareTask task, Instant at, String milestone, List<RecipientGroup> recipients) {
        return new CareEventContext("M04.TASK", task.id(), milestone, at, Optional.of(task.floorAreaId()), recipients);
    }
}
