package com.eldercare.backend.care.web.request;

import com.eldercare.backend.care.internal.result.domain.CareResultInput;
import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Schema(requiredProperties = {"residentId", "taskTypeId"})
public record AdHocCareRecordingWrite(
        Long residentId,
        Long taskTypeId,
        Instant performedAt,
        BigDecimal numericValue,
        String codeValue,
        Map<String, Object> jsonValue,
        Boolean upperLimitConfirmed,
        String note,
        Reason reason,
        Boolean sameTypeTasksReviewed,
        Long originalTaskId) {
    public CareResultInput input() {
        return new CareResultInput(
                performedAt,
                numericValue,
                codeValue,
                jsonValue,
                Boolean.TRUE.equals(upperLimitConfirmed),
                note,
                reason);
    }
}
