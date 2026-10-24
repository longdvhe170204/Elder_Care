package com.cambae.backend.m01.internal.consent.domain;

import com.cambae.backend.m01.api.dto.ConsenterType;
import java.time.Instant;
import java.util.Set;

/**
 * Bản đồng ý chia sẻ dữ liệu (nhóm 2, FR-010 → FR-016): nội dung không sửa; chỉ chuyển Hiệu lực → Đã rút lại.
 * {@code courtDecision} và {@code withdrawal} rỗng khi không áp dụng.
 */
public record Consent(
        long id,
        long residentId,
        ConsenterType consenterType,
        Long consenterFamilyMemberId,
        CourtDecision courtDecision,
        Set<Long> familyMemberIdsAllowedHealthView,
        boolean photoConsent,
        boolean notificationConsent,
        Instant consentedAt,
        long evidenceFileId,
        ConsentStatus status,
        Withdrawal withdrawal) {}
