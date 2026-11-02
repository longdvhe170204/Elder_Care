package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.common.error.UnmetCondition;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** T060: điều kiện tại thời điểm đọc, không thay thế kiểm tra khi xác nhận. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BedReadinessResponse(
        boolean allowed,
        boolean disinfectionRequired,
        ResidentSummarySchema departedResident,
        List<UnmetCondition> unmetConditions) {}
