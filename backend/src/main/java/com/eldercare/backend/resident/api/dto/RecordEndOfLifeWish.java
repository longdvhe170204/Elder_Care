package com.eldercare.backend.resident.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Lệnh ghi phiên bản phiếu nguyện vọng cuối đời (FR-023a → FR-023c, FR-023g). Dùng cho đường HTTP và cho 007 khi xác
 * nhận lại lúc nguy kịch ({@link WishRecordSource#CRITICAL_RECONFIRMATION}). {@code assessingDoctorId} là mã tài khoản
 * của Bác sĩ nhận định (vai trò AC-03 lúc ghi); {@code signerFamilyMemberId} chỉ có khi người ký không phải người cao
 * tuổi.
 */
public record RecordEndOfLifeWish(
        long residentId,
        WishChoice choice,
        String firstContact,
        String note,
        WishSignerType respondentType,
        Long respondentFamilyMemberId,
        String relationship,
        Long assessingDoctorId,
        SelfDeterminationAssessment assessment,
        WishSignerType signerType,
        Long signerFamilyMemberId,
        RepresentativeSelection representativeSelection,
        CourtDecisionRef courtDecision,
        List<WishOpinion> otherOpinions,
        Instant signedAt,
        Long signedCopyFileId) {}
