package com.eldercare.backend.care.api.events;

import com.eldercare.backend.common.notification.*;
import java.util.*;

final class CareEventNotifications {
    private static final String GENERAL_TITLE = "Thông tin chăm sóc cần xem xét";

    private CareEventNotifications() {}

    /** Tiêu đề riêng của sự kiện (không chứa thông tin sức khỏe, 009 FR-034); nội dung chung trùng tiêu đề. */
    static NotificationRequest titled(
            CareNotice event, NotificationLevel level, List<RecipientGroup> recipients, String key, String title) {
        return create(
                event,
                level,
                recipients,
                key,
                List.of(new ContentPart(InformationType.GENERAL, title)),
                Map.of(),
                Optional.empty(),
                title);
    }

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
        String title = GENERAL_TITLE;
        return create(
                event,
                level,
                recipients,
                key,
                List.of(new ContentPart(InformationType.GENERAL, title)),
                displayParams,
                Optional.empty(),
                GENERAL_TITLE);
    }

    static NotificationRequest create(
            CareNotice event,
            NotificationLevel level,
            List<RecipientGroup> recipients,
            String key,
            List<ContentPart> content) {
        return create(event, level, recipients, key, content, Map.of(), Optional.empty(), GENERAL_TITLE);
    }

    /** FR-071 (009 FR-001a): thông báo có nhóm người thân khai đối tượng thông tin; chỉ phần chung. */
    static NotificationRequest create(
            CareNotice event,
            NotificationLevel level,
            List<RecipientGroup> recipients,
            String key,
            Map<String, Object> displayParams,
            InformationSubject subject) {
        String title = GENERAL_TITLE;
        return create(
                event,
                level,
                recipients,
                key,
                List.of(new ContentPart(InformationType.GENERAL, title)),
                displayParams,
                Optional.of(subject),
                GENERAL_TITLE);
    }

    private static NotificationRequest create(
            CareNotice event,
            NotificationLevel level,
            List<RecipientGroup> recipients,
            String key,
            List<ContentPart> content,
            Map<String, Object> displayParams,
            Optional<InformationSubject> subject,
            String title) {
        var context = event.context();
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
