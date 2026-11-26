package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hồ sơ kết thúc lưu trú đã Hoàn tất (FR-033a, FR-072). {@code endDate} là ngày kết thúc thực tế.
 *
 * <p>Demo 2026-11-26 (E, chờ B duyệt): {@code familyText} khác rỗng thì người đại diện được báo kết thúc lưu trú (mức
 * Trung bình), nội dung gồm ngày kết thúc, lý do và bàn giao.
 */
public record ResidencyEndingCompleted(
        long residencyEndingId, long residentId, LocalDate endDate, Instant at, String familyText)
        implements HasNotificationRequests {

    public ResidencyEndingCompleted(long residencyEndingId, long residentId, LocalDate endDate) {
        this(residencyEndingId, residentId, endDate, null, null);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        if (familyText == null || at == null) return List.of();
        return List.of(new NotificationRequest(
                "M02",
                "M02_FAMILY_RESIDENCY_ENDED",
                SourceGroup.OTHER,
                new SourceRecord("M02.RESIDENCY_ENDING", String.valueOf(residencyEndingId)),
                "M02_FAMILY_RESIDENCY_ENDED:" + residencyEndingId,
                Optional.of(NotificationLevel.MEDIUM),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.Representative(residentId)),
                List.of(new ContentPart(InformationType.GENERAL, familyText)),
                Optional.of("Có cập nhật về lưu trú tại viện"),
                Optional.of("Kết thúc lưu trú"),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                Optional.empty()));
    }
}
