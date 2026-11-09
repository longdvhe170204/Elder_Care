package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.common.notification.*;
import java.time.*;
import java.util.*;

/** T004/C-13: sự kiện thông báo theo hợp đồng 005 mục 1.11. */
public record BatchedOverdueTaskReminder(
        long floorAreaId, int taskCount, Instant from, Instant to, Instant catchUpAt, CareEventContext context)
        implements CareNotice {
    /** Tương thích caller cũ; producer mới truyền khoảng dự kiến thật của nhóm. */
    public BatchedOverdueTaskReminder(List<Long> taskIds, CareEventContext context) {
        this(
                context.floorAreaId().orElseThrow(),
                taskIds.size(),
                context.occurredAt(),
                context.occurredAt(),
                context.occurredAt(),
                context);
    }

    /** Outbox cũ vẫn đọc được; payload mới không chứa danh sách id. */
    @com.fasterxml.jackson.annotation.JsonCreator
    public static BatchedOverdueTaskReminder restore(
            @com.fasterxml.jackson.annotation.JsonProperty("floorAreaId") Long floorAreaId,
            @com.fasterxml.jackson.annotation.JsonProperty("taskCount") Integer taskCount,
            @com.fasterxml.jackson.annotation.JsonProperty("from") Instant from,
            @com.fasterxml.jackson.annotation.JsonProperty("to") Instant to,
            @com.fasterxml.jackson.annotation.JsonProperty("catchUpAt") Instant catchUpAt,
            @com.fasterxml.jackson.annotation.JsonProperty("context") CareEventContext context,
            @com.fasterxml.jackson.annotation.JsonProperty("taskIds") List<Long> legacyIds) {
        return new BatchedOverdueTaskReminder(
                floorAreaId == null ? context.floorAreaId().orElseThrow() : floorAreaId,
                taskCount == null ? legacyIds.size() : taskCount,
                from == null ? context.occurredAt() : from,
                to == null ? context.occurredAt() : to,
                catchUpAt == null ? context.occurredAt() : catchUpAt,
                context);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(CareEventNotifications.create(
                this,
                level(),
                context.recipients(),
                eventKey(),
                Map.of(
                        "path",
                        "/care-tasks/overdue?floorAreaId=" + floorAreaId,
                        "taskCount",
                        taskCount,
                        "from",
                        from.toString(),
                        "to",
                        to.toString(),
                        "catchUpAt",
                        catchUpAt.toString())));
    }

    @Override
    public NotificationLevel level() {
        return NotificationLevel.LOW;
    }
}
