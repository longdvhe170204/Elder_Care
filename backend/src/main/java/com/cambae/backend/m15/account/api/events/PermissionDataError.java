package com.cambae.backend.m15.account.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceGroup;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/**
 * Lần đầu gặp dữ liệu quyền thiếu hoặc mâu thuẫn; báo Quản lý viện (FR-041b). {@code errorId} là dòng
 * {@code permission_data_error} đang mở.
 */
public record PermissionDataError(long errorId, String source, Long recordId, String errorKind, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "PERMISSION_DATA_ERROR",
                SourceGroup.OTHER,
                new SourceRecord("M15.PERMISSION_DATA_ERROR", String.valueOf(errorId)),
                "PERMISSION_DATA_ERROR:" + errorId,
                null,
                Notices.MANAGERS,
                "Lỗi dữ liệu quyền",
                "Dữ liệu nguồn " + source + " thiếu hoặc mâu thuẫn (" + errorKind
                        + "); thao tác cần dữ liệu này đang bị từ chối.",
                at));
    }
}
