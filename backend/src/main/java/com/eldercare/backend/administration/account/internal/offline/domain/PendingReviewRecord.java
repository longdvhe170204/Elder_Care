package com.eldercare.backend.administration.account.internal.offline.domain;

import java.time.Instant;

/**
 * Một bản ghi chờ xem lại ({@code simple_record} loại {@code M15.PENDING_REVIEW_RECORD}, data-model mục 0.1);
 * {@code content} là JSON gốc từ thiết bị.
 */
public record PendingReviewRecord(
        long id,
        String recordType,
        String content,
        String idempotencyKey,
        long recorderId,
        String deviceId,
        Instant deviceTime,
        Instant syncedAt,
        Instant lastConnectedAt,
        PendingReason pendingReason,
        Long floorAreaId,
        ReviewStatus status,
        Long decidedBy,
        Instant decidedAt,
        String rejectionReason,
        String reference,
        long version) {}
