package com.cambae.backend.m09.handover.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.*;

/** Hợp đồng 008; hiddenFields của cha chỉ rõ đường dẫn trường bị che ở con. */
@Schema(name = "Handover")
public record HandoverBody(
        long id,
        long shiftId,
        Long nextShiftId,
        long handoverStaffId,

        @Schema(allowableValues = {"DRAFT", "PREPARED", "HAS_COMMENTS", "CONFIRMED"})
        String status,

        Instant finalizedAt,
        Instant confirmedAt,
        String generalAssessment,
        List<Map<String, Object>> residentAssessments,
        List<Item> items,
        List<Comment> comments,
        List<String> allowedCommands,
        List<String> hiddenFields) {
    @Schema(name = "HandoverItem")
    public record Item(
            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            Long id,

            @Schema(
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    allowableValues = {
                        "TASK",
                        "DOSE",
                        "VITAL_SIGN",
                        "ALERT",
                        "INCIDENT",
                        "RESIDENT_CHANGE",
                        "BED_AWAITING_READINESS",
                        "STORED_BELONGING",
                        "FOLLOW_UP_ISSUE"
                    })
            String group,

            String sourceType,
            String sourceId,
            Long residentId,
            Long roomId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String description,

            String note,
            boolean resolved,

            @Schema(
                    accessMode = Schema.AccessMode.READ_ONLY,
                    allowableValues = {"FINALIZED", "AFTER_PREPARATION", "RESOLVED_DURING_SHIFT"})
            String section,

            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            String statusAtFinalization,

            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            String currentStatus,

            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            String noteAuthor) {}

    public record Comment(long id, String content, String sender, Instant at, List<Map<String, Object>> replies) {}

    @Schema(name = "HandoverSourceRecord")
    public record Source(
            @Schema(
                    allowableValues = {
                        "TASK",
                        "DOSE",
                        "VITAL_SIGN",
                        "ALERT",
                        "INCIDENT",
                        "RESIDENT_CHANGE",
                        "BED_AWAITING_READINESS",
                        "STORED_BELONGING"
                    })
            String group,

            String sourceType,
            String sourceId,
            Long residentId,
            Long roomId,
            String description,
            String currentStatus,
            boolean critical,
            boolean hasItem,
            boolean dataUnavailable) {}
}
