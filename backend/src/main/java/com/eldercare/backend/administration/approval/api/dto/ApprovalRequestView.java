package com.eldercare.backend.administration.approval.api.dto;

import com.eldercare.backend.administration.account.api.dto.Target;
import java.time.Instant;
import java.time.LocalDate;
import tools.jackson.databind.JsonNode;

/** Một yêu cầu phê duyệt (FR-032; data-model mục 3). {@code target} là đối tượng chính, dùng cho phạm vi duyệt (FR-034). */
public record ApprovalRequestView(
        long id,
        String displayCode,
        String type,
        String status,
        JsonNode content,
        Target target,
        long requesterId,
        Instant createdAt,
        String reason,
        LocalDate requestedEffectiveDate,
        LocalDate actualEffectiveDate,
        Instant submittedAt,
        Long decidedBy,
        Instant decidedAt,
        String decisionComment,
        boolean selfApproved,
        String closingReason,
        Instant appliedAt,
        boolean catchUpApplied,
        ResultRef applicationResult,
        Instant remindedAt,
        Instant escalatedAt,
        int version) {}
