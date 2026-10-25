package com.cambae.backend.m02.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/** Giường Trống cần chọn hồ sơ chờ; báo Hành chính, mức Nhẹ (FR-013, FR-015). {@code matchingEntryCount} bằng 0: không có người phù hợp. */
public record BedAvailableForWaitingList(long bedId, int matchingEntryCount, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        String text = matchingEntryCount == 0
                ? "Có giường Trống, không có hồ sơ chờ phù hợp."
                : "Có giường Trống, có " + matchingEntryCount + " hồ sơ chờ phù hợp cần chọn người để liên hệ.";
        return List.of(Notices.of(
                "BED_AVAILABLE_FOR_WAITING_LIST",
                new SourceRecord("M03.BED", String.valueOf(bedId)),
                "BED_AVAILABLE_FOR_WAITING_LIST:" + bedId + ":" + at.toEpochMilli(),
                NotificationLevel.LOW,
                null,
                Notices.OFFICE,
                "Giường Trống cần chọn hồ sơ chờ",
                text,
                at));
    }
}
