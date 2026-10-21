package com.cambae.backend.m01.api.dto;

import java.time.Instant;

/** Người cao tuổi quá hạn đánh giá lại (016 FR-044). */
public record OverdueReassessmentView(long residentId, Instant overdueSince) {}
