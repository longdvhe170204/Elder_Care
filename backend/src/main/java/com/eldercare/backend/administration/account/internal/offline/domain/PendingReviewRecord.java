package com.eldercare.backend.administration.account.internal.offline.domain;

import java.time.Instant;

/** Một dòng {@code pending_review_record} (data-model mục 5); {@code content} là JSON gốc từ thiết bị. */
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
