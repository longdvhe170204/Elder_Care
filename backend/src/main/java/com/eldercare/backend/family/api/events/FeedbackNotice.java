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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thông báo của phản hồi (FR-069; 012 US6): phản hồi mới, được giao lại, mở lại báo người phụ trách mức Trung bình (nội
 * bộ); đã được trả lời báo người gửi mức Nhẹ (về việc của chính người thân; nhóm "sức khỏe, thuốc" có phần sức khỏe,
 * người gửi không có quyền chỉ nhận phần chung); quá hạn leo thang Quản lý viện mức Trung bình một lần mỗi hạn
 * (BR-M10-05).
 *
 * @param occurrence phân biệt các lần của cùng loại trên một phản hồi (mã mục, hoặc hạn đã leo thang)
 * @param healthInformation trả lời của nhóm "sức khỏe, thuốc" (FR-058)
 */
public record FeedbackNotice(
        long feedbackId,
        long residentId,
        Kind kind,
        RecipientGroup recipient,
        boolean healthInformation,
        String occurrence,
        Instant at)
        implements HasNotificationRequests {

    public static final String SOURCE_TYPE = "M10.FEEDBACK";

    public enum Kind {
        ASSIGNED,
        REPLIED,
        ESCALATED
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        String title;
        List<ContentPart> content = new ArrayList<>();
        NotificationLevel level;
        Optional<InformationSubject> subject = Optional.empty();
        switch (kind) {
            case ASSIGNED -> {
                title = "Phản hồi cần xử lý";
                level = NotificationLevel.MEDIUM;
                content.add(new ContentPart(InformationType.GENERAL, title + ": xem và nhận xử lý trước hạn"));
            }
            case REPLIED -> {
                title = "Phản hồi đã được trả lời";
                level = NotificationLevel.LOW;
                subject = Optional.of(InformationSubject.ABOUT_FAMILY_MATTER);
                if (recipient instanceof RecipientGroup.Resident) {
                    // Người cao tuổi: chỉ phần chung, xem trên ứng dụng (FR-058a (b), FR-069; 009 FR-022a)
                    content.add(new ContentPart(
                            InformationType.GENERAL, "Phản hồi của bạn đã được trả lời: xem trong ứng dụng"));
                } else if (healthInformation) {
                    content.add(new ContentPart(
                            InformationType.HEALTH, title + ": xem nội dung trả lời trên cổng thông tin"));
                    content.add(new ContentPart(
                            InformationType.GENERAL,
                            title + "; nội dung là thông tin sức khỏe, vui lòng liên hệ trực tiếp với viện"));
                } else {
                    content.add(new ContentPart(
                            InformationType.GENERAL, title + ": xem nội dung trả lời trên cổng thông tin"));
                }
            }
            case ESCALATED -> {
                title = "Phản hồi quá hạn xử lý";
                level = NotificationLevel.MEDIUM;
                content.add(new ContentPart(InformationType.GENERAL, title + ": chưa được trả lời trước hạn"));
            }
            default -> throw new IllegalStateException(kind.name());
        }
        String summary = content.getLast().content();
        return List.of(new NotificationRequest(
                "M10",
                "FEEDBACK_" + kind.name(),
                SourceGroup.FAMILY_INFO,
                new SourceRecord(SOURCE_TYPE, String.valueOf(feedbackId)),
                SOURCE_TYPE + ":" + feedbackId + ":" + kind.name() + ":" + occurrence,
                Optional.of(level),
                Optional.of(residentId),
                Optional.empty(),
                List.of(recipient),
                List.copyOf(content),
                Optional.of(summary),
                Optional.of(title),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                subject,
                Optional.empty()));
    }
}
