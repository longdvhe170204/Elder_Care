package com.eldercare.backend.administration.approval.api;

import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Yêu cầu thông báo của sự kiện yêu cầu phê duyệt (009 contracts mục 1.1). Mức thông báo để trống vì mục 17 chưa quy
 * định cho nhóm này (009 FR-003: thiếu thì 009 coi là Nhẹ và gắn dấu); nội dung chỉ là thông tin chung, không có sức khỏe.
 */
final class Notices {

    static final String SOURCE_TYPE = "M15.APPROVAL_REQUEST";

    private Notices() {}

    static NotificationRequest of(
            ApprovalRequestView request, String eventType, List<RecipientGroup> recipients, String text, Instant at) {
        return new NotificationRequest(
                "M15",
                eventType,
                SourceGroup.OTHER,
                new SourceRecord(SOURCE_TYPE, String.valueOf(request.id())),
                eventType + ":" + request.id(),
                Optional.empty(),
                Optional.ofNullable(request.target().residentId()),
                Optional.ofNullable(request.target().floorAreaId()),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(text),
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
