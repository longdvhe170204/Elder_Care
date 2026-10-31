package com.eldercare.backend.notification.internal.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.notification.api.dto.AcknowledgementMethod;
import java.time.Instant;
import java.util.Set;

/**
 * Sự kiện của thông báo ở {@code record_event} loại {@code M13.NOTIFICATION_EVENT}, đối tượng {@code M13.NOTIFICATION}
 * (009 T043, data-model mục 0.1; FR-044, research B-09); người thực hiện ở {@code actor_id}, thời điểm ở
 * {@code occurred_at}. Gửi, không gửi, xem, xác nhận mỗi loại một lần ({@link #uniqueKey}).
 */
public record NotificationEventContent(
        NotificationEventType type,
        AcknowledgementMethod acknowledgementMethod,
        String reason,
        Instant scheduledSendAt) {

    public static final SharedKind<NotificationEventContent> KIND =
            SharedKind.of("M13.NOTIFICATION_EVENT", NotificationEventContent.class);

    /** Loại đối tượng của dòng bảng chung gắn với thông báo. */
    public static final String NOTIFICATION = "M13.NOTIFICATION";

    private static final Set<NotificationEventType> ONCE = Set.of(
            NotificationEventType.SEND,
            NotificationEventType.NOT_SENT,
            NotificationEventType.VIEW,
            NotificationEventType.ACKNOWLEDGE);

    public NotificationEventContent {
        if ((type == NotificationEventType.ACKNOWLEDGE) != (acknowledgementMethod != null)) {
            throw new IllegalArgumentException("Cách xác nhận có khi và chỉ khi sự kiện xác nhận");
        }
        if ((type == NotificationEventType.HOLD || type == NotificationEventType.RESCHEDULE)
                && scheduledSendAt == null) {
            throw new IllegalArgumentException("Giữ, đổi lịch gửi cần thời điểm gửi dự kiến");
        }
    }

    /** {@code M13.NE:<thông báo>:<loại>} với loại chỉ một lần; loại khác {@code null}. */
    public static String uniqueKey(long notificationId, NotificationEventType type) {
        return ONCE.contains(type) ? "M13.NE:" + notificationId + ":" + type.name() : null;
    }
}
