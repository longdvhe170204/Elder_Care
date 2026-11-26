package com.eldercare.backend.admission.web.request;

import com.eldercare.backend.admission.internal.ending.domain.RecipientType;
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

        // Bỏ trống: chính người ghi (BF-10 bước 6, phương án A 2026-11-24; Hành chính không xem được hồ sơ nhân viên)
        Long handoverStaffId,

        Long pickupRecordId) {}
