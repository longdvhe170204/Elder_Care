package com.eldercare.backend.billing.internal.procurement.domain;

import com.eldercare.backend.billing.internal.procurement.domain.ProcurementRequestRow.Line;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Nội dung của yêu cầu mua sắm, sửa được khi Nháp (FR-008); khóa khi rời Nháp (trigger của V019_02). */
public record ProcurementContent(
        long departmentId,
        ProcurementType type,
        String priority,
        Instant neededBy,
        List<Long> relatedFileIds,
        List<Line> lines,
        long estimatedTotal,
        String reason,
        LocalDate neededOn,
        Map<String, Object> reference,
        Long originalRequestId) {}
