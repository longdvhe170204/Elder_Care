package com.eldercare.backend.notification.internal.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Lần gửi theo kênh ở {@code record_event} loại {@code M13.DELIVERY_ATTEMPT}, đối tượng {@code M13.NOTIFICATION} (009
 * T043; FR-018): thời điểm dự kiến ở {@code occurred_at}; {@code attemptNo} = 1 + số lần gửi lại, mỗi (thông báo, kênh,
 * số lần) một dòng ({@link #uniqueKey}); {@code batchWindowId} là khung gộp tin dồn vào (FR-016b), {@code content} là nội
 * dung riêng (tin tổng hợp).
 */
public record DeliveryAttemptContent(
        String channel, String address, int attemptNo, Long batchWindowId, String content) {

    public static final SharedKind<DeliveryAttemptContent> KIND =
            SharedKind.of("M13.DELIVERY_ATTEMPT", DeliveryAttemptContent.class);

    public static String uniqueKey(long notificationId, String channel, int attemptNo) {
        return "M13.DA:" + notificationId + ":" + channel + ":" + attemptNo;
    }
}
