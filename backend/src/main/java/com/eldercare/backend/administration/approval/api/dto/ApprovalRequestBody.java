package com.eldercare.backend.administration.approval.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.LongFunction;
import tools.jackson.databind.JsonNode;

/** Yêu cầu theo schema {@code ApprovalRequest}. */
public record ApprovalRequestBody(
        long id,
        String displayCode,
        String type,

        @Schema(
                allowableValues = {
                    "DRAFT",
                    "PENDING_APPROVAL",
                    "APPROVED",
                    "APPLIED",
                    "APPLY_FAILED",
                    "REJECTED",
                    "CANCELLED"
                })
        String status,

        @Schema(implementation = Object.class) JsonNode content,
        ActorBody requester,
        Instant createdAt,
        String reason,
        LocalDate requestedEffectiveDate,
        LocalDate actualEffectiveDate,
        Instant submittedAt,
        ActorBody decidedBy,
        Instant decidedAt,
        String decisionComment,
        boolean selfApproved,
        String closingReason,
        Instant appliedAt,
        boolean catchUpApplied,
        ResultRef applicationResult,
        List<String> allowedCommands,
        int version) {

    public static ApprovalRequestBody of(ApprovalRequestDetails viewed, LongFunction<String> names) {
        ApprovalRequestView r = viewed.request();
        return new ApprovalRequestBody(
                r.id(),
                r.displayCode(),
                r.type(),
                r.status(),
                r.content(),
                ActorBody.user(r.requesterId(), names),
                r.createdAt(),
                r.reason(),
                r.requestedEffectiveDate(),
                r.actualEffectiveDate(),
                r.submittedAt(),
                ActorBody.user(r.decidedBy(), names),
                r.decidedAt(),
                r.decisionComment(),
                r.selfApproved(),
                r.closingReason(),
                r.appliedAt(),
                r.catchUpApplied(),
                r.applicationResult(),
                viewed.allowedCommands(),
                r.version());
    }
}
