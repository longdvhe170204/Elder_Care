package com.eldercare.backend.administration.account.internal.offline.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * Nội dung của bản ghi chờ xem lại ở {@code simple_record} loại {@code M15.PENDING_REVIEW_RECORD} (002 T098; FR-041c):
 * trạng thái ở cột {@code status}, khóa lặp lại ở {@code unique_key} ({@link #uniqueKey}), dòng đã quyết định khóa.
 */
public record PendingReviewContent(
        String recordType,
        JsonNode content,
        String idempotencyKey,
        long recorderId,
        String deviceId,
        Instant deviceTime,
        Instant syncedAt,
        Instant lastConnectedAt,
        PendingReason pendingReason,
        Long floorAreaId,
        Long decidedBy,
        Instant decidedAt,
        String rejectionReason,
        String resultReference) {

    public static final SharedKind<PendingReviewContent> KIND =
            SharedKind.of("M15.PENDING_REVIEW_RECORD", PendingReviewContent.class);

    /** Mỗi người ghi, mỗi khóa lặp lại một bản ghi. */
    public static String uniqueKey(long recorderId, String idempotencyKey) {
        return "M15.PRR:" + recorderId + ":" + idempotencyKey;
    }

    public PendingReviewContent decided(long by, Instant at, String rejection, String reference) {
        return new PendingReviewContent(
                recordType,
                content,
                idempotencyKey,
                recorderId,
                deviceId,
                deviceTime,
                syncedAt,
                lastConnectedAt,
                pendingReason,
                floorAreaId,
                by,
                at,
                rejection,
                reference);
    }
}
