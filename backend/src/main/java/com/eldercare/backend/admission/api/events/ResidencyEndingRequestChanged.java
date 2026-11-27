package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.common.Role;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Yêu cầu kết thúc lưu trú của người đại diện đổi trạng thái (demo 2026-11-26, E làm, chờ B duyệt). {@code change}:
 * {@code SUBMITTED}, {@code CANCELLED} báo Hành chính (nút Xử lý ở web mở hồ sơ điền sẵn); {@code HANDLED},
 * {@code REJECTED} báo người đại diện. Nguồn {@code M02.ENDING_REQUEST}.
 */
public record ResidencyEndingRequestChanged(long requestId, long residentId, String change, String text, Instant at)
        implements HasNotificationRequests {

    public static final String SOURCE_TYPE = "M02.ENDING_REQUEST";

    @Override
    public List<NotificationRequest> notificationRequests() {
        boolean toFamily = "HANDLED".equals(change) || "REJECTED".equals(change);
        return List.of(new NotificationRequest(
                "M02",
                "M02_ENDING_REQUEST_" + change,
                SourceGroup.OTHER,
                new SourceRecord(SOURCE_TYPE, String.valueOf(requestId)),
                "M02_ENDING_REQUEST:" + change + ":" + requestId,
                Optional.of(NotificationLevel.MEDIUM),
                Optional.of(residentId),
                Optional.empty(),
                List.of(
                        toFamily
                                ? new RecipientGroup.Representative(residentId)
                                : new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)),
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(text),
                Optional.of("Yêu cầu kết thúc lưu trú"),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                toFamily ? Optional.of(InformationSubject.ABOUT_FAMILY_MATTER) : Optional.empty(),
                Optional.empty()));
    }
}
