package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.api.dto.Importance;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Q-453: một việc Nháp của công việc tháng. {@code performerId} rỗng là việc chung của tầng. */
@Schema(name = "CareMonthDraft")
public record CareMonthDraftResponse(
        long id,
        Long residentId,
        long taskTypeId,
        long shiftId,
        Instant plannedAt,
        Importance importance,
        Long performerId) {
    public static CareMonthDraftResponse of(CareTask t) {
        return new CareMonthDraftResponse(
                t.id(), t.residentId(), t.taskTypeId(), t.shiftId(), t.plannedAt(), t.importance(), t.performerId());
    }
}
