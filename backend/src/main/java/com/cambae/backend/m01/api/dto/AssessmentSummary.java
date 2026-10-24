package com.cambae.backend.m01.api.dto;

import java.time.Instant;
import java.util.Set;

/** Tóm tắt lần đánh giá Đã xác nhận (004 FR-004 (a)). */
public record AssessmentSummary(
        long id, long residentId, CareLevel careLevel, Set<CareNeedView> careNeeds, Instant confirmedAt) {}
