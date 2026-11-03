package com.eldercare.backend.care.api.events;

import com.eldercare.backend.common.notification.*;
import java.util.*;

final class CareEventNotifications {
    private CareEventNotifications() {}

    static NotificationRequest create(
            CareNotice event, NotificationLevel level, List<RecipientGroup> recipients, String key) {
        String title = "Thông tin chăm sóc cần xem xét";
        return create(event, level, recipients, key, List.of(new ContentPart(InformationType.GENERAL, title)));
    }

    static NotificationRequest create(
            CareNotice event,
            NotificationLevel level,
            List<RecipientGroup> recipients,
            String key,
            List<ContentPart> content) {
        var context = event.context();
        String title = "Thông tin chăm sóc cần xem xét";
        return new NotificationRequest(
                "M04",
                event.getClass().getSimpleName(),
                SourceGroup.OTHER,
                new SourceRecord(context.sourceType(), Long.toString(context.sourceId())),
                key,
                Optional.of(level),
                event.notificationResidentId(),
                context.floorAreaId(),
                recipients,
                content,
                Optional.of(title),
                Optional.of(title),
                Optional.empty(),
                false,
                context.occurredAt(),
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty());
    }
}
