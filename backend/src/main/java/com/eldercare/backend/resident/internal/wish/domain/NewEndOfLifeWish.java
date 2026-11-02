package com.eldercare.backend.resident.internal.wish.domain;

import com.eldercare.backend.resident.api.dto.CourtDecisionRef;
import com.eldercare.backend.resident.api.dto.RepresentativeSelection;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import java.time.Instant;
import java.time.LocalDate;

/** Dữ liệu một phiên bản mới của phiếu nguyện vọng cuối đời; {@code recordedAt} và {@code assessedAt} từ đồng hồ nghiệp vụ. */
public record NewEndOfLifeWish(
        long residentId,
        WishChoice choice,
        String firstContact,
        String note,
        WishSignerType respondentType,
        Long respondentFamilyMemberId,
        String relationship,
        long assessingDoctorId,
        boolean canSelfDetermine,
        Instant assessedAt,
        String supportMethod,
        LocalDate reviewDate,
        WishSignerType signerType,
        Long signerFamilyMemberId,
        RepresentativeSelection representativeSelection,
        CourtDecisionRef courtDecision,
        Instant signedAt,
        long signedCopyFileId,
        WishRecordSource source,
        long recordedBy,
        Instant recordedAt) {}
