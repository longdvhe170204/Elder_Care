package com.cambae.backend.m02.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/** Tới ngày xem xét lại giữ giường; báo Quản lý viện, mức Nhẹ (FR-055, FR-056). */
public record BedHoldReviewDue(long absenceId, long residentId, Instant at) implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "BED_HOLD_REVIEW_DUE",
                new SourceRecord("M02.ABSENCE", String.valueOf(absenceId)),
                "BED_HOLD_REVIEW_DUE:" + absenceId + ":" + at.toEpochMilli(),
                NotificationLevel.LOW,
                residentId,
                Notices.MANAGER,
                "Tới ngày xem xét lại giữ giường",
                "Tới ngày xem xét lại quyết định giữ giường của lượt vắng.",
                at));
    }
}
