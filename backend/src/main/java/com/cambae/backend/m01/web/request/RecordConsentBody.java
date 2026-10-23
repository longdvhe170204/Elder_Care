package com.cambae.backend.m01.web.request;

import com.cambae.backend.m01.api.dto.ConsenterType;
import com.cambae.backend.m01.internal.consent.domain.CourtDecision;
import com.cambae.backend.m01.internal.consent.service.ConsentService.RecordConsent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * Thân lệnh ghi nhận bản đồng ý (contracts/api-001.yaml, POST {@code /residents/{id}/consents}; FR-010, FR-011).
 * {@code evidenceFileId} không gắn {@code @NotNull} để thiếu bản ký trả 422 {@code EVIDENCE_REQUIRED} thay vì 400.
 */
public record RecordConsentBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        ConsenterType consenterType,

        @Schema(description = "Bắt buộc khi LEGAL_REPRESENTATIVE")
        Long consenterFamilyMemberId,

        CourtDecisionBody courtDecision,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Set<Long> familyMemberIdsAllowedHealthView,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean photoConsent,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean notificationConsent,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Instant consentedAt,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long evidenceFileId) {

    /** Bằng chứng quyết định của Tòa án (schema {@code CourtDecision}); trường thiếu bị từ chối ở dịch vụ. */
    @Schema(name = "CourtDecision")
    public record CourtDecisionBody(String number, LocalDate date, String court, Long fileId) {}

    public RecordConsent command() {
        CourtDecision decision = courtDecision == null
                ? null
                : new CourtDecision(
                        courtDecision.number(), courtDecision.date(), courtDecision.court(), courtDecision.fileId());
        return new RecordConsent(
                consenterType,
                consenterFamilyMemberId,
                decision,
                familyMemberIdsAllowedHealthView,
                photoConsent,
                notificationConsent,
                consentedAt,
                evidenceFileId);
    }
}
