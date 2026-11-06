package com.eldercare.backend.family.api.events;

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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lượt thăm Đã duyệt bị hệ thống hủy (FR-036, FR-037): người đăng ký nhận thông báo, mức Trung bình khi khu đang khoanh
 * vùng, Nhẹ khi người cao tuổi không có mặt tại viện. Kết thúc quan hệ hủy lượt nhưng không báo (người đó không còn
 * quan hệ).
 *
 * @param cause {@code ISOLATION_ACTIVE} hoặc {@code RESIDENT_NOT_PRESENT}
 */
public record VisitCancelledNotice(
        long visitId,
        long residentId,
        long familyMemberId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String cause,
        Instant at)
        implements HasNotificationRequests {

    public static final String SOURCE_TYPE = "M10.VISIT";
    private static final String TITLE = "Lượt thăm đã hủy";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public List<NotificationRequest> notificationRequests() {
        boolean isolation = "ISOLATION_ACTIVE".equals(cause);
        String why = isolation ? "khu đang khoanh vùng" : "người cao tuổi không có mặt tại viện";
        String summary = TITLE + " " + startTime + "–" + endTime + " ngày " + date.format(DAY) + ": " + why
                + ". Có thể đăng ký lại khung khác.";
        return List.of(new NotificationRequest(
                "M10",
                "VISIT_CANCELLED",
                SourceGroup.FAMILY_INFO,
                new SourceRecord(SOURCE_TYPE, String.valueOf(visitId)),
                SOURCE_TYPE + ":" + visitId + ":CANCELLED",
                Optional.of(isolation ? NotificationLevel.MEDIUM : NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.FamilyMember(familyMemberId, "VISIT_REGISTRANT")),
                List.of(new ContentPart(InformationType.VISITS, summary)),
                Optional.of(summary),
                Optional.of(TITLE),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                Optional.empty()));
    }
}
