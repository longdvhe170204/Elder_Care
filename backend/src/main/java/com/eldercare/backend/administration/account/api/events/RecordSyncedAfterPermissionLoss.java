package com.eldercare.backend.administration.account.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/** Bản ghi ngoại tuyến được chấp nhận kèm dấu vì mất quyền lúc đồng bộ (FR-041a, CHK056); báo người xem lại của tầng. */
public record RecordSyncedAfterPermissionLoss(String recordType, String recordId, Long floorAreaId, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "RECORD_SYNCED_AFTER_PERMISSION_LOSS",
                SourceGroup.OTHER,
                new SourceRecord(recordType, recordId),
                "RECORD_SYNCED_AFTER_PERMISSION_LOSS:" + recordType + ":" + recordId,
                floorAreaId,
                Notices.reviewers(floorAreaId),
                "Bản ghi đồng bộ sau khi mất quyền",
                "Một bản ghi ngoại tuyến được nhận kèm dấu: người ghi đã mất quyền trước khi đồng bộ.",
                at));
    }
}
