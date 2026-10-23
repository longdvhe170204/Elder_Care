package com.cambae.backend.m01.web.response;

import com.cambae.backend.m01.api.dto.ConsenterType;
import com.cambae.backend.m01.internal.consent.domain.Consent;
import com.cambae.backend.m01.internal.consent.domain.ConsentStatus;
import com.cambae.backend.m01.internal.consent.domain.CourtDecision;
import com.cambae.backend.m01.internal.consent.domain.Withdrawal;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/** Bản đồng ý (contracts/api-001.yaml, schema {@code Consent}). */
@Schema(name = "Consent")
public record ConsentBody(
        long id,
        ConsenterType consenterType,
        Long consenterFamilyMemberId,
        CourtDecisionBody courtDecision,
        Set<Long> familyMemberIdsAllowedHealthView,
        boolean photoConsent,
        boolean notificationConsent,
        Instant consentedAt,
        long evidenceFileId,
        ConsentStatus status,
        WithdrawalBody withdrawal) {

    /** Lý do hiện cho bản được thay thế (không có lý do do người nhập). */
    static final String SUPERSEDED_REASON = "Được thay thế bởi bản đồng ý mới";

    @Schema(name = "CourtDecision")
    public record CourtDecisionBody(String number, LocalDate date, String court, Long fileId) {}

    public record WithdrawalBody(Instant at, String reason, Long evidenceFileId, Long supersededById) {}

    public static ConsentBody of(Consent consent) {
        CourtDecision court = consent.courtDecision();
        Withdrawal w = consent.withdrawal();
        return new ConsentBody(
                consent.id(),
                consent.consenterType(),
                consent.consenterFamilyMemberId(),
                court == null
                        ? null
                        : new CourtDecisionBody(court.number(), court.date(), court.court(), court.fileId()),
                consent.familyMemberIdsAllowedHealthView(),
                consent.photoConsent(),
                consent.notificationConsent(),
                consent.consentedAt(),
                consent.evidenceFileId(),
                consent.status(),
                w == null
                        ? null
                        : new WithdrawalBody(
                                w.at(),
                                w.reason() == null ? SUPERSEDED_REASON : w.reason(),
                                w.evidenceFileId(),
                                w.supersededById()));
    }
}
