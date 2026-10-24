package com.cambae.backend.m13.web.response;

import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.m13.api.dto.AcknowledgementMethod;
import com.cambae.backend.m13.api.dto.NotificationStatus;
import com.cambae.backend.m13.internal.inbox.service.InboxService.Opened;
import java.time.Instant;
import java.util.List;

/**
 * Thông báo trong hộp theo schema {@code Notification} của contracts/api-009.yaml: nội dung theo quyền hiện hành, phần bị
 * bỏ liệt kê ở {@code hiddenParts} (FR-034). Liên kết bản ghi nguồn chưa mở được (chờ khóa quyền xem của module nguồn);
 * "Xác nhận và tiếp nhận" làm ở US1.
 */
public record NotificationBody(
        long id,
        NotificationLevel level,
        String eventType,
        String title,
        List<Part> content,
        List<String> hiddenParts,
        boolean identityOnly,
        NotificationStatus status,
        Instant sentAt,
        Instant viewedAt,
        Instant acknowledgedAt,
        AcknowledgementMethod acknowledgementMethod,
        Source sourceRecord,
        boolean canAcknowledgeAndTakeCharge) {

    public record Part(InformationType informationType, String content) {}

    public record Source(String type, String id, boolean canOpen) {}

    public static NotificationBody of(Opened o) {
        var r = o.row();
        return new NotificationBody(
                r.id(),
                r.level(),
                r.eventType(),
                r.title(),
                o.content().stream().map(p -> new Part(p.type(), p.content())).toList(),
                o.hiddenParts().stream().map(Enum::name).toList(),
                r.identityOnly(),
                r.status(),
                r.sentAt(),
                r.viewedAt(),
                r.acknowledgedAt(),
                r.acknowledgementMethod(),
                new Source(r.sourceRecordType(), r.sourceRecordId(), false),
                false);
    }
}
