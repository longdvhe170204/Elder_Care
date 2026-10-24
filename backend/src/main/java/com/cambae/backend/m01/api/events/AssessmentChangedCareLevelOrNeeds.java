package com.cambae.backend.m01.api.events;

import com.cambae.backend.m01.api.dto.CareLevel;
import java.util.Set;

/** Đánh giá đổi mức hoặc nhu cầu chăm sóc; 005 nghe (BR-M04-20; FR-043). */
public record AssessmentChangedCareLevelOrNeeds(
        long residentId,
        long assessmentId,
        CareLevel previousLevel,
        CareLevel newLevel,
        Set<Long> previousNeedIds,
        Set<Long> newNeedIds) {}
