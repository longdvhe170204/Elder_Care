package com.eldercare.backend.belongings.internal.belonging.domain;

import com.eldercare.backend.belongings.api.events.BelongingNotice;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationSubject;
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
 * Dựng yêu cầu thông báo của M12 (FR-032, research K-13): nội dung chỉ loại sự kiện và loại đồ (SC-012); có nhóm người
 * thân thì khai "về việc của chính người thân".
 */
public final class BelongingNotices {

    private BelongingNotices() {}

    public static BelongingNotice notice(
            String eventType,
            String sourceType,
            long sourceId,
            NotificationLevel level,
            long residentId,
            List<RecipientGroup> recipients,
            String text,
            Long causedBy,
            Instant at) {
        boolean toFamily = recipients.stream()
                .anyMatch(g -> g instanceof RecipientGroup.Representative
                        || g instanceof RecipientGroup.PrimaryContact
                        || g instanceof RecipientGroup.FamilyMember
                        || g instanceof RecipientGroup.FamilyWithRight);
        return new BelongingNotice(List.of(new NotificationRequest(
                "M12",
                eventType,
                SourceGroup.OTHER,
                new SourceRecord(sourceType, Long.toString(sourceId)),
                eventType + ":" + sourceId,
                Optional.of(level),
                Optional.of(residentId),
                Optional.empty(),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(text),
                Optional.of("Đồ gửi"),
                Optional.ofNullable(causedBy),
                false,
                at,
                Map.of(),
                false,
                toFamily ? Optional.of(InformationSubject.ABOUT_FAMILY_MATTER) : Optional.empty(),
                Optional.empty())));
    }
}
