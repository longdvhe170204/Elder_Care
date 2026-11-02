package com.eldercare.backend.resident.internal.assessment.domain;

import java.time.Instant;

/** Một lần báo cần đánh giá lại chưa được đáp ứng. */
public record ReassessmentNotice(
        long id,
        ReassessmentBasis basis,
        String sourceType,
        long sourceId,
        Instant notifiedAt,
        Instant deadlineFrom,
        Instant overdueAlertedAt) {}
