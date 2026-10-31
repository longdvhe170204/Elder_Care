package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.common.notification.*;
import java.time.*;
import java.util.*;

/** T004/C-13: sự kiện thông báo theo hợp đồng 005 mục 1.11. */
public record CarePlanReturned(long planId, long residentId, CareEventContext context, String reason)
        implements CareNotice {
    /** Giữ tương thích với caller và sự kiện cũ chưa mang lý do. */
    public CarePlanReturned(long planId, long residentId, CareEventContext context) {
        this(planId, residentId, context, null);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        if (reason == null || reason.isBlank()) return CareNotice.super.notificationRequests();
        return List.of(CareEventNotifications.create(
                this,
                level(),
                context.recipients(),
                eventKey(),
                List.of(new ContentPart(InformationType.HEALTH, reason))));
    }

    @Override
    public NotificationLevel level() {
        return NotificationLevel.LOW;
    }

    @Override
    public Optional<Long> notificationResidentId() {
        return Optional.of(residentId);
    }
}
