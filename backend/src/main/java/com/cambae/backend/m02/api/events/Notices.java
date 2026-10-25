package com.cambae.backend.m02.api.events;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.ContentPart;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.SourceGroup;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Yêu cầu thông báo của sự kiện 004 (research T-12; 009 contracts mục 1.1). Mức theo nguyên tắc 3 bậc; mọi thông báo
 * gửi nhân viên, nội dung chỉ thông tin chung (không sức khỏe, không chi phí chi tiết).
 */
final class Notices {

    static final RecipientGroup OFFICE = new RecipientGroup.FacilityRole(Role.OFFICE_STAFF);
    static final RecipientGroup MANAGER = new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER);

    private Notices() {}

    static NotificationRequest of(
            String eventType,
            SourceRecord source,
            String eventKey,
            NotificationLevel level,
            Long residentId,
            RecipientGroup recipients,
            String title,
            String text,
            Instant at) {
        return new NotificationRequest(
                "M02",
                eventType,
                SourceGroup.OTHER,
                source,
                eventKey,
                Optional.of(level),
                Optional.ofNullable(residentId),
                Optional.empty(),
                List.of(recipients),
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(text),
                Optional.of(title),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty());
    }
}
