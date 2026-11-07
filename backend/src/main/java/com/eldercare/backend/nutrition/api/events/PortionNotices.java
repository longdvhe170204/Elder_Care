package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Khuôn yêu cầu thông báo của chốt suất (FR-065): bản ghi nguồn là bữa ngày; nội dung không có tên người, dị ứng, thành phần
 * (SC-008, SC-014). Thông báo gắn một người (điều dưỡng phụ trách) mang {@code residentId}.
 */
final class PortionNotices {

    private PortionNotices() {}

    static NotificationRequest request(
            String eventType,
            long dailyMealId,
            String eventKey,
            NotificationLevel level,
            Long residentId,
            List<RecipientGroup> recipients,
            String text,
            String title,
            Long causedBy,
            Instant at) {
        return new NotificationRequest(
                "M08",
                eventType,
                SourceGroup.OTHER,
                new SourceRecord("M08.DAILY_MEAL", String.valueOf(dailyMealId)),
                eventKey,
                Optional.of(level),
                Optional.ofNullable(residentId),
                Optional.empty(),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.empty(),
                Optional.of(title),
                Optional.ofNullable(causedBy),
                false,
                at,
                Map.of("dailyMealId", dailyMealId),
                false,
                Optional.empty(),
                Optional.empty());
    }
}
