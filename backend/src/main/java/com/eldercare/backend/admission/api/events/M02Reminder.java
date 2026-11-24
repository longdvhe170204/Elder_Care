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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lần nhắc Hành chính của M02, mức Nhẹ (FR-017, FR-021, FR-026, FR-029, FR-066a, FR-072, FR-076). {@code type} theo
 * glossary mục 10.6 (ví dụ {@code CONTRACT_EXPIRING}); {@code on} là ngày nhắc nên mỗi ngày một thông báo.
 *
 * <p>Demo 2026-11-25 (góp ý nhóm, E làm, chờ B, A duyệt): {@code detail} là nội dung đọc được cho Hành chính (thiếu thì
 * dùng câu chung như trước); {@code familyText} khác rỗng thì người đại diện của {@code residentId} được báo thêm, mức
 * Trung bình, phần "hợp đồng".
 */
public record M02Reminder(
        String type, long subjectId, LocalDate on, Instant at, Long residentId, String detail, String familyText)
        implements HasNotificationRequests {

    public M02Reminder(String type, long subjectId, LocalDate on, Instant at) {
        this(type, subjectId, on, at, null, null, null);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        var source = new SourceRecord("M02." + type, String.valueOf(subjectId));
        var requests = new ArrayList<NotificationRequest>();
        requests.add(Notices.of(
                "M02_REMINDER_" + type,
                source,
                "M02_REMINDER:" + type + ":" + subjectId + ":" + on,
                NotificationLevel.LOW,
                residentId,
                Notices.OFFICE,
                "Nhắc việc tiếp nhận và lưu trú",
                detail != null ? detail : "Có việc cần Hành chính xử lý (" + type + ").",
                at));
        if (familyText != null && residentId != null)
            requests.add(new NotificationRequest(
                    "M02",
                    "M02_FAMILY_" + type,
                    SourceGroup.OTHER,
                    source,
                    "M02_FAMILY:" + type + ":" + subjectId + ":" + on,
                    Optional.of(NotificationLevel.MEDIUM),
                    Optional.of(residentId),
                    Optional.empty(),
                    List.of(new RecipientGroup.Representative(residentId)),
                    List.of(new ContentPart(InformationType.CONTRACT, familyText)),
                    Optional.of("Có cập nhật về hợp đồng lưu trú"),
                    Optional.of("Hợp đồng lưu trú"),
                    Optional.empty(),
                    false,
                    at,
                    Map.of(),
                    false,
                    Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                    Optional.empty()));
        return List.copyOf(requests);
    }
}
