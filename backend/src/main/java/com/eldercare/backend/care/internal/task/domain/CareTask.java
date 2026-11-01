package com.eldercare.backend.care.internal.task.domain;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.DataGroup;
import java.time.Instant;

/** T021: công việc có trạng thái; snapshot dữ liệu riêng trong data. */
@DataGroup(DataGroup.Group.BUSINESS)
public record CareTask(
        long id,
        Long residentId,
        String locationType,
        Long locationId,
        long taskTypeId,
        SourceType basisType,
        Long basisId,
        Long planId,
        Long originalTaskId,
        long shiftId,
        long floorAreaId,
        Instant plannedAt,
        Instant windowFrom,
        Instant windowTo,
        Importance importance,
        Role performerRole,
        Long performerId,
        TaskStatus status,
        CareTaskData data,
        long createdBy,
        Instant createdAt,
        int version) {}
