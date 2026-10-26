package com.cambae.backend.m02.web.request;

import com.cambae.backend.m02.internal.ending.domain.RecipientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Thân Ghi nhận bàn giao người cao tuổi (contracts/api-004.yaml, POST {@code /residency-endings/{id}/handover}; FR-063 (e)):
 * người thân nhận cần {@code familyMemberId}, cơ sở tiếp nhận cần {@code facilityName}.
 */
public record ResidentHandoverBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        RecipientType recipientType,

        Long familyMemberId,

        @Size(max = 255) String facilityName,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Instant handoverAt,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long handoverStaffId) {}
