package com.cambae.backend.m07.api.events;

import com.cambae.backend.common.notification.ContentPart;
import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.InformationSubject;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.SourceGroup;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Báo gia đình thuốc gửi sắp hết ({@code ThuocGuiSapHet}, FR-046, Q-393): Điều dưỡng ghi lệnh trên một lô Được sử
 * dụng; 009 báo người liên hệ chính và người đại diện mức Trung bình (Q-116), phần "chung" kèm ghi chú, phần "sức khỏe"
 * là tên thuốc. Thông báo về việc của chính người thân vì họ là người gửi thuốc (Q-362). Mỗi lần báo một khóa sự kiện.
 */
public record FamilyMedicationRunningLow(
        long batchId, long residentId, String medicationName, String note, long reportedBy, Instant reportedAt)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        List<ContentPart> content = new ArrayList<>();
        content.add(new ContentPart(
                InformationType.GENERAL,
                "Thuốc gia đình gửi sắp hết, đề nghị gia đình liên hệ viện"
                        + (note == null || note.isBlank() ? "." : ": " + note)));
        content.add(new ContentPart(InformationType.HEALTH, "Thuốc: " + medicationName));
        return List.of(new NotificationRequest(
                "M07",
                "FAMILY_MEDICATION_RUNNING_LOW",
                SourceGroup.MEDICATION,
                new SourceRecord("M07.FAMILY_MEDICATION_BATCH", String.valueOf(batchId)),
                "FAMILY_MEDICATION_RUNNING_LOW:" + batchId + ":" + reportedAt.toEpochMilli(),
                Optional.of(NotificationLevel.MEDIUM),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.PrimaryContact(residentId), new RecipientGroup.Representative(residentId)),
                content,
                Optional.empty(),
                Optional.of("Thuốc gia đình gửi sắp hết"),
                Optional.of(reportedBy),
                false,
                reportedAt,
                Map.of("batchId", batchId),
                false,
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                Optional.empty()));
    }
}
