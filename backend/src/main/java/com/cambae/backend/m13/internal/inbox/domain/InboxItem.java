package com.cambae.backend.m13.internal.inbox.domain;

import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.m13.api.dto.AcknowledgementMethod;
import com.cambae.backend.m13.api.dto.NotificationStatus;
import com.cambae.backend.m13.api.dto.RecipientType;
import java.time.Instant;
import java.util.Set;

/** Một thông báo trong hộp, kèm dữ liệu của yêu cầu để hiển thị theo quyền hiện hành. */
public record InboxItem(
        long id,
        long requestId,
        RecipientType recipientType,
        long recipientId,
        NotificationLevel level,
        String eventType,
        String title,
        Set<InformationType> visibleParts,
        boolean identityOnly,
        NotificationStatus status,
        Instant sentAt,
        Instant viewedAt,
        Instant acknowledgedAt,
        AcknowledgementMethod acknowledgementMethod,
        boolean hidden,
        Long residentId,
        Long floorAreaId,
        String sourceRecordType,
        String sourceRecordId) {

    /** Khẩn cấp chưa xác nhận: đặt trên cùng (FR-033). */
    public boolean urgentUnacknowledged() {
        return level == NotificationLevel.URGENT && acknowledgedAt == null;
    }
}
