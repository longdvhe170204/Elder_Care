package com.eldercare.backend.notification.internal.delivery.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Khung gộp tin nhắn Trung bình ở {@code simple_record} loại {@code M13.SMS_BATCH_WINDOW} (009 T043; FR-016b): trạng thái
 * {@code OPEN}, {@code CLOSED}; một khung mở mỗi người nhận ({@link #openKey} ở {@code unique_key}, nhả khi đóng).
 */
public record SmsBatchWindowContent(
        String recipientKey,
        Instant startedAt,
        Instant endsAt,
        long firstAttemptId,
        Instant closedAt,
        Long digestAttemptId) {

    public static final SharedKind<SmsBatchWindowContent> KIND =
            SharedKind.of("M13.SMS_BATCH_WINDOW", SmsBatchWindowContent.class);

    public static final String OPEN = "OPEN";
    public static final String CLOSED = "CLOSED";

    public static String openKey(String recipientKey) {
        return "M13.SBW:" + recipientKey;
    }

    public SmsBatchWindowContent closed(Instant at, Long digest) {
        return new SmsBatchWindowContent(recipientKey, startedAt, endsAt, firstAttemptId, at, digest);
    }
}
