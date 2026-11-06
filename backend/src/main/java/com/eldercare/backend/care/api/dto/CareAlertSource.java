package com.eldercare.backend.care.api.dto;

import java.time.Instant;

/** T019/FR-050: đích vị trí và thông tin công việc nguồn của cảnh báo quá hạn. */
public record CareAlertSource(
        String locationType,
        Long locationId,
        long floorAreaId,
        long taskTypeId,
        String taskTypeName,
        Instant plannedAt,
        Instant windowFrom,
        Instant windowTo,
        Importance importance) {}
