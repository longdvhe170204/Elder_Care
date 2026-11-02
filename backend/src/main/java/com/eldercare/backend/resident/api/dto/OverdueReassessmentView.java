package com.eldercare.backend.resident.api.dto;

import java.time.Instant;

/** Người cao tuổi quá hạn đánh giá lại (016 FR-044). */
public record OverdueReassessmentView(long residentId, Instant overdueSince) {}
