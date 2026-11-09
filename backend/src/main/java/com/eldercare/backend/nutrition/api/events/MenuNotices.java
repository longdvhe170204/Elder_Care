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

/** Khuôn yêu cầu thông báo của thực đơn tuần (FR-065): chỉ tới nhân viên, nội dung không có tên người, dị ứng. */
final class MenuNotices {

    static final String MENU = "M08.MENU";

    private MenuNotices() {}

    static NotificationRequest request(
            String eventType,
            long menuId,
            String eventKey,
            NotificationLevel level,
            List<RecipientGroup> recipients,
            String text,
            String title,
            Long causedBy,
            Instant at) {
        return new NotificationRequest(
                "M08",
                eventType,
                SourceGroup.OTHER,
                new SourceRecord(MENU, String.valueOf(menuId)),
                eventKey,
                Optional.of(level),
                Optional.empty(),
                Optional.empty(),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.empty(),
                Optional.of(title),
                Optional.ofNullable(causedBy),
                false,
                at,
                Map.of("menuId", menuId),
                false,
                Optional.empty(),
                Optional.empty());
    }
}
