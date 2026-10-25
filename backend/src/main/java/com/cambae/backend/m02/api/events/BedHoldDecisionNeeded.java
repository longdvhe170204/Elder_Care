package com.cambae.backend.m02.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/** Tới ngưỡng giữ giường, cần Quản lý viện quyết định; mức Trung bình (FR-055, FR-056, FR-061). */
public record BedHoldDecisionNeeded(long absenceId, long residentId, int days, String policyLine, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "BED_HOLD_DECISION_NEEDED",
                new SourceRecord("M02.ABSENCE", String.valueOf(absenceId)),
                "BED_HOLD_DECISION_NEEDED:" + absenceId + ":" + days,
                NotificationLevel.MEDIUM,
                residentId,
                Notices.MANAGER,
                "Cần quyết định giữ giường",
                "Lượt vắng đã " + days + " ngày, tới ngưỡng giữ giường, cần ghi quyết định.",
                at));
    }
}
