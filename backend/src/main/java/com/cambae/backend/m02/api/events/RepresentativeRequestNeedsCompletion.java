package com.cambae.backend.m02.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/** Yêu cầu do người đại diện gửi ở Nháp cần Hành chính hoàn thiện; mức Nhẹ (FR-039). */
public record RepresentativeRequestNeedsCompletion(long requestId, long residentId, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "REPRESENTATIVE_REQUEST_NEEDS_COMPLETION",
                new SourceRecord("M02.RESIDENCY_CHANGE", String.valueOf(requestId)),
                "REPRESENTATIVE_REQUEST_NEEDS_COMPLETION:" + requestId,
                NotificationLevel.LOW,
                residentId,
                Notices.OFFICE,
                "Yêu cầu của người đại diện cần hoàn thiện",
                "Người đại diện gửi yêu cầu thay đổi lưu trú, Hành chính cần hoàn thiện rồi gửi duyệt.",
                at));
    }
}
