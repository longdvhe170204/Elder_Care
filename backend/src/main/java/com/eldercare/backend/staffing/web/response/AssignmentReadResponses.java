package com.eldercare.backend.staffing.web.response;

import com.eldercare.backend.staffing.internal.assignment.service.AssignmentReadService;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public final class AssignmentReadResponses {
    private AssignmentReadResponses() {}

    @Schema(
            name = "AssignmentResolution",
            requiredProperties = {
                "shiftId",
                "residentId",
                "at",
                "role",
                "taskTypeId",
                "primaryStaffId",
                "assignedNurseStaffIds"
            })
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Resolution(
            long shiftId,
            long residentId,
            Instant at,
            String role,

            @Schema(
                    types = {"integer", "null"},
                    format = "int64")
            Long taskTypeId,

            @Schema(
                    types = {"integer", "null"},
                    format = "int64")
            Long primaryStaffId,

            @io.swagger.v3.oas.annotations.media.ArraySchema(uniqueItems = true)
            List<Long> assignedNurseStaffIds) {
        public static Resolution of(AssignmentReadService.Resolution r) {
            return new Resolution(
                    r.shiftId(),
                    r.residentId(),
                    r.at(),
                    r.role().name(),
                    r.taskTypeId(),
                    r.primaryStaffId(),
                    r.assignedNurseStaffIds());
        }
    }

    @Schema(
            name = "UnassignedResident",
            requiredProperties = {"residentId", "missingAssignedNurse", "missingPrimaryCaregiver"})
    public record Missing(long residentId, boolean missingAssignedNurse, boolean missingPrimaryCaregiver) {}

    @Schema(
            name = "UnassignedResidents",
            requiredProperties = {"shiftId", "evaluatedAt", "items"})
    public record Unassigned(long shiftId, Instant evaluatedAt, List<Missing> items) {
        public static Unassigned of(AssignmentReadService.Unassigned r) {
            return new Unassigned(
                    r.shiftId(),
                    r.evaluatedAt(),
                    r.items().stream()
                            .map(i ->
                                    new Missing(i.residentId(), i.missingAssignedNurse(), i.missingPrimaryCaregiver()))
                            .toList());
        }
    }
}
