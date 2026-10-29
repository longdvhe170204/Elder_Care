package com.eldercare.backend.resident.api.events;

import com.eldercare.backend.resident.api.dto.CareLevel;
import java.util.Set;

/** Đánh giá đổi mức hoặc nhu cầu chăm sóc; 005 nghe (BR-M04-20; FR-043). */
public record AssessmentChangedCareLevelOrNeeds(
        long residentId,
        long assessmentId,
        CareLevel previousLevel,
        CareLevel newLevel,
        Set<Long> previousNeedIds,
        Set<Long> newNeedIds) {}
