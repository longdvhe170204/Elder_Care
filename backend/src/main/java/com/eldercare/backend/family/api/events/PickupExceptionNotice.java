package com.eldercare.backend.family.api.events;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Yêu cầu ngoại lệ đón cần xác nhận (FR-024; US2 kịch bản 3): người đại diện mức Trung bình (chịu giờ yên tĩnh của 009),
 * Quản lý viện nhận ngay để duyệt.
 */
public record PickupExceptionNotice(
        Kind kind, long residentId, long exceptionId, String pickerName, List<Long> representativeIds, Instant at)
        implements HasNotificationRequests {

    public static final String SOURCE_TYPE = "M10.PICKUP_EXCEPTION";

    public enum Kind {
        NEEDS_CONFIRMATION
    }

    public PickupExceptionNotice {
        representativeIds = List.copyOf(representativeIds);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        List<RecipientGroup> recipients = new ArrayList<>();
        representativeIds.forEach(id -> recipients.add(new RecipientGroup.FamilyMember(id, "REPRESENTATIVE")));
        recipients.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        String title = "Ngoại lệ đón cần xác nhận";
        String summary = title + ": " + pickerName + " không thuộc danh sách được phép đón";
        return List.of(new NotificationRequest(
                "M10",
                "PICKUP_EXCEPTION_NEEDS_CONFIRMATION",
                SourceGroup.FAMILY_INFO,
                new SourceRecord(SOURCE_TYPE, String.valueOf(exceptionId)),
                SOURCE_TYPE + ":" + exceptionId + ":" + kind,
                Optional.of(NotificationLevel.MEDIUM),
                Optional.of(residentId),
                Optional.empty(),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, summary)),
                Optional.of(summary),
                Optional.of(title),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                Optional.empty()));
    }
}
