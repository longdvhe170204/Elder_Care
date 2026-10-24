package com.cambae.backend.m09.shift.web.response;

import com.cambae.backend.m09.shift.internal.domain.*;
import com.cambae.backend.m09.shift.internal.service.port.PublicationWarning;
import com.cambae.backend.m09.web.response.M09ProblemBody.M09Warning;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Schema đọc theo api-008; version chỉ đọc được chủ dự án duyệt 2026-10-24. */
public final class ShiftResponses {
    private ShiftResponses() {}

    @Schema(name = "ShiftTemplate")
    public record Template(
            long id,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String name,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^[0-2][0-9]:[0-5][0-9]$")
            String startTime,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^[0-2][0-9]:[0-5][0-9]$")
            String endTime,

            @Schema(
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    allowableValues = {"FLOOR", "AREA", "FACILITY"})
            String scopeType,

            boolean handoverRequired,
            boolean active,

            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            long version) {
        public static Template of(ShiftTemplate t) {
            return new Template(
                    t.id(),
                    t.name(),
                    t.startTime().toString(),
                    t.endTime().toString(),
                    t.scopeType(),
                    t.handoverRequired(),
                    t.active(),
                    t.version());
        }
    }

    @Schema(name = "ShiftSchedule")
    public record Schedule(
            long id,
            String month,
            String scopeType,
            Long scopeId,

            @Schema(allowableValues = {"DRAFT", "PUBLISHED"})
            String status,

            List<Shift> shifts,

            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            long version) {}

    public record Staff(long staffId, boolean onDutyDoctor, boolean absent, boolean approvedLeave) {}

    @Schema(name = "StaffingRatioCalculation")
    public record StaffingRatio(
            Instant at,
            String event,
            BigDecimal totalWeight,
            int staffCount,
            BigDecimal ratio,
            BigDecimal threshold,
            boolean meetsThreshold) {}

    @Schema(name = "Shift")
    public record Shift(
            long id,
            long shiftTemplateId,
            Instant startsAt,
            Instant endsAt,

            @Schema(allowableValues = {"DRAFT", "PUBLISHED", "IN_PROGRESS", "AWAITING_HANDOVER", "CLOSED", "CANCELLED"})
            String status,

            Long leadStaffId,
            boolean missingLead,
            List<Staff> staff,
            StaffingRatio staffingRatio,
            List<String> allowedCommands,

            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            long version) {}

    @Schema(name = "ResultWithWarnings")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public record Result(Long id, List<M09Warning> warnings) {}

    public record InvalidAssignment(long assignmentId, String reason) {}

    @Schema(name = "SchedulePublicationPreview")
    public record Preview(
            List<Object> unmetConditions, List<InvalidAssignment> invalidAssignments, List<M09Warning> warnings) {}

    public static List<M09Warning> warnings(List<PublicationWarning> warnings) {
        return warnings.stream()
                .map(w -> new M09Warning(w.code(), w.description(), w.details()))
                .toList();
    }
}
