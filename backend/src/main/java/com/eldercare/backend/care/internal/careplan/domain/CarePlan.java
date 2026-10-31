package com.eldercare.backend.care.internal.careplan.domain;

import com.eldercare.backend.care.api.dto.CarePlanStatus;
import com.eldercare.backend.common.data.DataGroup;
import java.time.*;

/** Snapshot một phiên bản, chỉ persistence ghi lại qua lệnh nghiệp vụ. */
@DataGroup(DataGroup.Group.BUSINESS)
public record CarePlan(
        long id,
        long residentId,
        int revision,
        CarePlanStatus status,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Long previousPlanId,
        String changeReason,
        long createdBy,
        Instant createdAt,
        Long approvedBy,
        Instant approvedAt,
        boolean selfApproved,
        Instant submittedAt,
        Instant remindedAt,
        Instant escalatedAt,
        String endReason,
        int version) {}
