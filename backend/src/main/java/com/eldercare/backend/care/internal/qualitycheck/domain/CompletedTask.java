package com.eldercare.backend.care.internal.qualitycheck.domain;

import java.time.Instant;

/** Công việc Hoàn thành/Hoàn thành trễ của ca (005); người ghi kết quả là người thực hiện (FR-060). */
public record CompletedTask(
        long id,
        Long performerStaffId,
        long recordedBy,
        Instant performedAt,
        String basisType,
        long shiftId,
        long taskTypeId,
        Long residentId,
        Long locationId) {}
