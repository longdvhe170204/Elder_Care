package com.eldercare.backend.resident.internal.wish.domain;

import com.eldercare.backend.resident.api.dto.CourtDecisionRef;
import com.eldercare.backend.resident.api.dto.RepresentativeSelection;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishOpinion;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import java.time.Instant;
import java.util.List;

/**
 * Nội dung bản Nháp tự khai của phiếu nguyện vọng cuối đời do Hành chính ghi (Q-412, FR-023n): mọi trường trừ người trả
 * lời để trống được; không có nhận định tự quyết (chỉ Bác sĩ, BR-M01-15), Bác sĩ, Điều dưỡng bổ sung khi kiểm chứng.
 */
public record EndOfLifeDraftContent(
        WishChoice choice,
        String firstContact,
        String note,
        WishSignerType respondentType,
        Long respondentFamilyMemberId,
        String relationship,
        WishSignerType signerType,
        Long signerFamilyMemberId,
        RepresentativeSelection representativeSelection,
        CourtDecisionRef courtDecision,
        List<WishOpinion> otherOpinions,
        Instant signedAt,
        Long signedCopyFileId) {}
