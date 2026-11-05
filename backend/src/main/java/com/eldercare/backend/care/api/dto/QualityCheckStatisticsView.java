package com.eldercare.backend.care.api.dto;

import java.time.Instant;

/** 014 ActivityStatisticsApi.qualityChecks (FR-065); result PASS, FAIL, NOT_CHECKABLE, OVERDUE. */
public record QualityCheckStatisticsView(
        long itemId,
        long shiftId,
        long floorAreaId,
        long careTaskId,
        String result,
        String checkerRole,
        Instant checkedAt) {}
