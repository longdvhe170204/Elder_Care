package com.eldercare.backend.care.web.request;

import com.eldercare.backend.care.internal.session.service.SessionAttendanceService;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** 014 T014: thân {@code SessionAttendance} của api-014 (glossary 20.3). */
public record SessionAttendanceWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @ArraySchema(minItems = 1)
        List<SessionAttendanceItem> results,

        Instant deviceAt) {
    public record SessionAttendanceItem(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long residentId,

            Boolean addedAtAttendance,

            @Schema(
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    allowableValues = {"PRESENT", "ABSENT"})
            String result,

            @Schema(allowableValues = {"REFUSED", "HEALTH", "AWAY", "NO_SHOW", "OTHER"})
            String absenceReason,

            @Schema(allowableValues = {"ACTIVE", "NORMAL", "PASSIVE", "LEFT_EARLY"})
            String participationLevel,

            @Schema(allowableValues = {"PROACTIVE", "WHEN_ASKED", "MINIMAL"})
            String communicationLevel,

            String conditionAfter,
            String note) {}

    public List<SessionAttendanceService.Item> toItems() {
        return results == null
                ? List.of()
                : results.stream()
                        .map(r -> new SessionAttendanceService.Item(
                                r.residentId(),
                                r.addedAtAttendance(),
                                r.result(),
                                r.absenceReason(),
                                r.participationLevel(),
                                r.communicationLevel(),
                                r.conditionAfter(),
                                r.note()))
                        .toList();
    }
}
