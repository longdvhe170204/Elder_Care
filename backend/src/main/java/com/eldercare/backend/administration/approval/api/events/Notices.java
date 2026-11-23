package com.eldercare.backend.administration.approval.api.events;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.common.notification.ContentPart;
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
 * Yêu cầu thông báo của sự kiện yêu cầu phê duyệt (009 contracts mục 1.1). Mức theo bảng mức của 009 (dòng 000): có yêu
 * cầu mới Nhẹ; chờ duyệt quá CFG-M15-05, CFG-M15-06 và Áp dụng không thành Trung bình (sửa 2026-11-23, trước để trống nên
 * 009 gắn dấu thiếu mức); nội dung chỉ là thông tin chung, không có sức khỏe.
 */
final class Notices {

    static final String SOURCE_TYPE = "M15.APPROVAL_REQUEST";

    private Notices() {}

    private static NotificationLevel levelOf(String eventType) {
        return switch (eventType) {
            case "REQUEST_REMINDER", "REQUEST_ESCALATION", "REQUEST_APPLY_FAILED" -> NotificationLevel.MEDIUM;
            default -> NotificationLevel.LOW;
        };
    }

    static NotificationRequest of(
            ApprovalRequestView request, String eventType, List<RecipientGroup> recipients, String text, Instant at) {
        return of(request, eventType, recipients, text, text, at);
    }

    /** {@code text} là nội dung đầy đủ; {@code shortText} là nội dung rút gọn (kênh ngoài ứng dụng). */
    static NotificationRequest of(
            ApprovalRequestView request,
            String eventType,
            List<RecipientGroup> recipients,
            String text,
            String shortText,
            Instant at) {
        return new NotificationRequest(
                "M15",
                eventType,
                SourceGroup.OTHER,
                new SourceRecord(SOURCE_TYPE, String.valueOf(request.id())),
                eventType + ":" + request.id(),
                Optional.of(levelOf(eventType)),
                Optional.ofNullable(request.target().residentId()),
                Optional.ofNullable(request.target().floorAreaId()),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(shortText),
                Optional.of("Yêu cầu " + request.displayCode()),
                Optional.empty(),
                false,
                at,
                Map.of("displayCode", request.displayCode(), "type", request.type()),
                false,
                Optional.empty(),
                Optional.empty());
    }
}
