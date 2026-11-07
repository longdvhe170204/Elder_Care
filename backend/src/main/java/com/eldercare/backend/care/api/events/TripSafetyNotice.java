package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.common.notification.NotificationLevel;

/** FR-021a/035: nhắc người chịu trách nhiệm chuyến, không mang chi tiết sức khỏe trong thông báo chung. */
public record TripSafetyNotice(long tripId, String cause, CareEventContext context) implements CareNotice {
    @Override
    public NotificationLevel level() {
        return NotificationLevel.MEDIUM;
    }
}
