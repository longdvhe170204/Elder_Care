package com.cambae.backend.m15.account.web.response;

import com.cambae.backend.m15.account.internal.offline.domain.PendingReason;
import com.cambae.backend.m15.account.internal.offline.domain.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(name = "PendingReviewRecord")
public record PendingReviewRecordBody(
        Long id,
        String recordType,
        Map<String, Object> content,
        Long recorderId,
        String recorderName,
        Instant deviceTime,
        Instant syncedAt,
        Instant lastConnectedAt,
        PendingReason pendingReason,
        Long floorAreaId,
        String floorAreaName,
        ReviewStatus status,
        Long decidedBy,
        Instant decidedAt,
        String rejectionReason,
        String reference,
        Long version) {}
