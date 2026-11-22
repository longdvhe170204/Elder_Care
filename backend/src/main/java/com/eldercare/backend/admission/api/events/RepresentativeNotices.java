package com.eldercare.backend.admission.api.events;

import com.eldercare.backend.common.notification.ContentPart;
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
 * Thông báo của 004 gửi người đại diện về thay đổi lưu trú (phụ lục áp dụng, đề nghị xác nhận, nhắc xác nhận): mức Nhẹ,
 * nội dung loại thay đổi và ngày, không kèm giá.
 */
final class RepresentativeNotices {

    private static final Map<String, String> LABELS = Map.of(
            "RESIDENCE_TYPE", "loại lưu trú",
            "CARE_LEVEL", "mức chăm sóc",
            "SERVICE", "dịch vụ",
            "DURATION", "thời hạn",
            "BED", "giường",
            "ARRIVAL_SCHEDULE", "lịch đến");

    private RepresentativeNotices() {}

    static String changes(Set<String> changeTypes) {
        return changeTypes.stream().sorted().map(t -> LABELS.getOrDefault(t, t)).collect(Collectors.joining(", "));
    }

    static String day(LocalDate date) {
        return "%02d/%02d/%d".formatted(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    static NotificationRequest of(
            String eventType,
            SourceRecord source,
            String eventKey,
            long residentId,
            String title,
            String text,
            Instant at) {
        return new NotificationRequest(
                "M02",
                eventType,
                SourceGroup.OTHER,
                source,
                eventKey,
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.Representative(residentId)),
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(text),
                Optional.of(title),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.of(InformationSubject.ABOUT_RESIDENT),
                Optional.empty());
    }
}
