package com.eldercare.backend.staffing.assignment.web.response;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.assignment.internal.domain.*;
import com.eldercare.backend.staffing.assignment.web.request.AssignmentRequests;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.*;
import java.util.List;

public final class AssignmentResponses {
    private AssignmentResponses() {}

    @Schema(name = "FloorManagerAssignment")
    public record Floor(
            long id,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            long staffId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            long floorAreaId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            LocalDate from,

            LocalDate to,
            String reason) {
        public static Floor of(FloorManagerAssignment a) {
            return new Floor(
                    a.id(),
                    a.staffId(),
                    a.floorAreaId(),
                    LocalDate.ofInstant(a.startsAt(), BusinessTime.ZONE),
                    a.endsAt() == null
                            ? null
                            : LocalDate.ofInstant(a.endsAt(), BusinessTime.ZONE).minusDays(1),
                    a.reason());
        }
    }

    @Schema(
            name = "Assignment",
            allOf = {AssignmentRequests.New.class})
    public record Assignment(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            long staffId,

            @Schema(
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    allowableValues = {"FLOOR", "AREA", "ROOM", "RESIDENT"})
            String targetType,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            long targetId,

            Long taskTypeId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String role,

            @Schema(
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    allowableValues = {"PRIMARY", "SUPPORT"})
            String assignmentKind,

            boolean assignedNurse,
            Instant startsAt,
            Instant endsAt,
            long id,

            @Schema(allowableValues = {"DRAFT", "SCHEDULED", "ACTIVE", "ENDED", "CANCELLED"})
            String status,

            Instant actualEndAt,

            @Schema(allowableValues = {"MANUAL", "COPIED", "SHIFT_SWAP_015"})
            String source) {
        public static Assignment of(StaffAssignment a) {
            return new Assignment(
                    a.staffId(),
                    a.targetType(),
                    a.targetId(),
                    a.taskTypeId(),
                    a.role().name(),
                    a.assignmentKind(),
                    a.assignedNurse(),
                    a.startsAt(),
                    a.endsAt(),
                    a.id(),
                    a.status(),
                    a.actualEndAt(),
                    a.source());
        }
    }

    public record Skipped(long sourceAssignmentId, String reason) {}

    @Schema(name = "CopyAssignmentsResult")
    public record Copy(List<Long> copiedIds, List<Skipped> notCopied) {}
}
