package com.eldercare.backend.care.api.events;

import com.eldercare.backend.common.notification.*;
import java.util.*;

final class CareEventNotifications {
    private CareEventNotifications() {}

    static NotificationRequest create(
            CareNotice event, NotificationLevel level, List<RecipientGroup> recipients, String key) {
        return create(event, level, recipients, key, Map.of());
    }

    static NotificationRequest create(
            CareNotice event,
            NotificationLevel level,
            List<RecipientGroup> recipients,
            String key,
            Map<String, Object> displayParams) {
        String title = "Thông tin chăm sóc cần xem xét";
        return create(
                event,
                level,
                recipients,
                key,
                List.of(new ContentPart(InformationType.GENERAL, title)),
                displayParams,
                Optional.empty());
    }

    static NotificationRequest create(
            CareNotice event,
            NotificationLevel level,
            List<RecipientGroup> recipients,
            String key,
            List<ContentPart> content) {
        return create(event, level, recipients, key, content, Map.of(), Optional.empty());
    }

    /** FR-071 (009 FR-001a): thông báo có nhóm người thân khai đối tượng thông tin; chỉ phần chung. */
    static NotificationRequest create(
            CareNotice event,
            NotificationLevel level,
            List<RecipientGroup> recipients,
            String key,
            Map<String, Object> displayParams,
            InformationSubject subject) {
        String title = "Thông tin chăm sóc cần xem xét";
        return create(
                event,
                level,
                recipients,
                key,
                List.of(new ContentPart(InformationType.GENERAL, title)),
                displayParams,
                Optional.of(subject));
    }

    private static NotificationRequest create(
            CareNotice event,
            NotificationLevel level,
            List<RecipientGroup> recipients,
            String key,
            List<ContentPart> content,
            Map<String, Object> displayParams,
            Optional<InformationSubject> subject) {
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
                displayParams,
                false,
                subject,
                Optional.empty());
    }
}
