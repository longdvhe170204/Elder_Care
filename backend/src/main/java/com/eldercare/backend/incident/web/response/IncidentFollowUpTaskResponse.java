package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.care.api.dto.Importance;
import com.eldercare.backend.care.api.dto.SourceTaskView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Schema {@code IncidentFollowUpTask} của api-007 (FR-045b). */
@Schema(name = "IncidentFollowUpTask")
public record IncidentFollowUpTaskResponse(
        long id,
        String basisType,
        long shiftId,
        String taskType,
        String content,
        Instant plannedAt,
        Instant dueAt,
        Importance importance,

        @Schema(types = {"string", "null"}) String performer,

        boolean shared,

        @Schema(
                allowableValues = {
                    "NOT_YET_DUE",
                    "DUE",
                    "OVERDUE",
                    "COMPLETED",
                    "COMPLETED_LATE",
                    "NOT_PERFORMED",
                    "CANCELLED"
                })
        String status,

        @Schema(
                types = {"string", "null"},
                format = "date-time")
        Instant closedAt,

        @Schema(types = {"string", "null"}) String resultNote,

        @Schema(types = {"string", "null"}) String closureReason) {
    public static IncidentFollowUpTaskResponse of(SourceTaskView task) {
        return new IncidentFollowUpTaskResponse(
                task.id(),
                task.basisType().name(),
                task.shiftId(),
                task.taskTypeName(),
                task.content(),
                task.plannedAt(),
                task.windowTo(),
                task.importance(),
                task.performer(),
                task.shared(),
                task.status().name(),
                task.closedAt(),
                task.resultNote(),
                task.closureReason());
    }
}
