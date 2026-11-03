package com.eldercare.backend.care.internal.task.domain;

import com.eldercare.backend.care.api.dto.Importance;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import java.time.Instant;

/** Nội dung lệnh do người lập nhập; chưa phải công việc được lưu. */
public record CareTaskDraft(
        Long residentId,
        String locationType,
        Long locationId,
        long taskTypeId,
        SourceType basisType,
        Long basisId,
        String content,
        Instant plannedAt,
        Integer windowBeforeMinutes,
        Integer windowAfterMinutes,
        Importance importance,
        Long performerId,
        boolean shared) {}
