package com.eldercare.backend.care.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/** Một dòng trong tab “Đã duyệt” (FR-059, Q-451). */
@Schema(
        name = "CarePlanApprovedRow",
        requiredProperties = {
            "residentId",
            "fullName",
            "profileCode",
            "planId",
            "status",
            "approvedBy",
            "approvedAt",
            "effectiveFrom",
            "selfApproved"
        })
public record CarePlanApprovedResponse(
        long residentId,
        String fullName,
        String profileCode,
        long planId,

        @Schema(allowableValues = {"EFFECTIVE", "PENDING_EFFECT", "EXPIRED", "CANCELLED"})
        String status,

        @Schema(types = {"string", "null"}) String approvedBy,
        Instant approvedAt,

        @Schema(
                types = {"string", "null"},
                format = "date")
        LocalDate effectiveFrom,

        boolean selfApproved) {}
