package com.cambae.backend.m15.account.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceGroup;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/** Bản ghi ngoại tuyến vào chờ xem lại (FR-041c); báo người xem lại của tầng. */
public record RecordPendingReview(long pendingRecordId, Long floorAreaId, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "RECORD_PENDING_REVIEW",
                SourceGroup.OTHER,
                new SourceRecord("M15.PENDING_REVIEW_RECORD", String.valueOf(pendingRecordId)),
                "RECORD_PENDING_REVIEW:" + pendingRecordId,
                floorAreaId,
                Notices.reviewers(floorAreaId),
                "Bản ghi chờ xem lại",
                "Có bản ghi ngoại tuyến chờ xem lại trước khi được nhận.",
                at));
    }
}
