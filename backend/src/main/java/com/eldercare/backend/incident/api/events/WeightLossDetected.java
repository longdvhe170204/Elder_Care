package com.eldercare.backend.incident.api.events;

import java.time.Instant;

/** Phát hiện sụt cân để 011 đánh giá dinh dưỡng (FR-039a). */
public record WeightLossDetected(
        long residentId, long highestRecordId, long latestRecordId, String eventKey, Instant occurredAt) {}
