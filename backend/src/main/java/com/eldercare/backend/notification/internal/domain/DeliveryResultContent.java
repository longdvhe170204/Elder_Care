package com.eldercare.backend.notification.internal.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Kết quả lần gửi ở {@code record_event} loại {@code M13.DELIVERY_RESULT}, đối tượng là lần gửi
 * ({@link DeliveryAttemptContent#KIND}) (009 T043): một kết quả mỗi lần gửi ({@link #uniqueKey}); {@code digestAttemptId}
 * là lần gửi tin tổng hợp với {@code BATCHED}.
 */
public record DeliveryResultContent(DeliveryOutcome result, String reason, Long digestAttemptId) {

    public static final SharedKind<DeliveryResultContent> KIND =
            SharedKind.of("M13.DELIVERY_RESULT", DeliveryResultContent.class);

    public static String uniqueKey(long attemptId) {
        return "M13.DR:" + attemptId;
    }
}
