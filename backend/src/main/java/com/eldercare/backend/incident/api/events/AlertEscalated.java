package com.eldercare.backend.incident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/**
 * Cảnh báo hoặc sự cố quá hạn tiếp nhận đã lên cấp leo thang (FR-033, FR-044; hợp đồng 007 mục 3). {@code sourceType} là
 * {@code M05.ALERT} hoặc {@code M05.INCIDENT}, {@code sourceId} mã tương ứng ("Đổi api" chủ dự án chốt 2026-11-07).
 */
public record AlertEscalated(
        String sourceType,
        long sourceId,
        int escalationLevel,
        String eventKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public AlertEscalated {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
