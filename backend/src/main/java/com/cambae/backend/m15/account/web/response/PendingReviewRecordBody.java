package com.cambae.backend.m15.account.web.response;

import com.cambae.backend.m15.account.internal.offline.PendingReason;
import com.cambae.backend.m15.account.internal.offline.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(name = "PendingReviewRecord")
public record PendingReviewRecordBody(
        Long id,
        String recordType,
        Map<String, Object> content,
        Long recorderId,
        Instant deviceTime,
        Instant syncedAt,
        Instant lastConnectedAt,
        PendingReason pendingReason,
        Long floorAreaId,
        ReviewStatus status,
        Long decidedBy,
        Instant decidedAt,
        String rejectionReason,
        String reference) {}
