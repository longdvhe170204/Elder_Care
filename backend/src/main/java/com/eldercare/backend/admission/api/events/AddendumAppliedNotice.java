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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Phụ lục đã áp dụng: báo người đại diện nội dung và ngày hiệu lực, mức Nhẹ, không kèm giá (đề xuất D 2026-11-18, chờ chủ dự
 * án duyệt; team-plan mục 8).
 */
public record AddendumAppliedNotice(
        long addendumId, long residentId, Set<String> changeTypes, LocalDate effectiveDate, Instant at)
        implements HasNotificationRequests {

    private static final Map<String, String> LABELS = Map.of(
            "RESIDENCE_TYPE", "loại lưu trú",
            "CARE_LEVEL", "mức chăm sóc",
            "SERVICE", "dịch vụ",
            "DURATION", "thời hạn",
            "BED", "giường",
            "ARRIVAL_SCHEDULE", "lịch đến");

    @Override
    public List<NotificationRequest> notificationRequests() {
        String changes = changeTypes.stream()
                .sorted()
                .map(t -> LABELS.getOrDefault(t, t))
                .collect(Collectors.joining(", "));
        String day = "%02d/%02d/%d"
                .formatted(effectiveDate.getDayOfMonth(), effectiveDate.getMonthValue(), effectiveDate.getYear());
        String text = "Phụ lục hợp đồng thay đổi " + changes + " có hiệu lực từ " + day + ".";
        return List.of(new NotificationRequest(
                "M02",
                "ADDENDUM_APPLIED",
                SourceGroup.OTHER,
                new SourceRecord("M02.ADDENDUM", String.valueOf(addendumId)),
                "ADDENDUM_APPLIED:" + addendumId,
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.Representative(residentId)),
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(text),
                Optional.of("Phụ lục hợp đồng có hiệu lực"),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.of(InformationSubject.ABOUT_RESIDENT),
                Optional.empty()));
    }
}
