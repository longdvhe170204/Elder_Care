package com.eldercare.backend.resident.internal.wish.domain;

import com.eldercare.backend.resident.api.dto.CourtDecisionRef;
import com.eldercare.backend.resident.api.dto.RepresentativeSelection;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Một phiên bản phiếu nguyện vọng cuối đời (data-model mục 6; FR-023a). */
public record EndOfLifeWish(
        long id,
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
        List<RecordedOpinion> otherOpinions,
        Instant signedAt,
        long signedCopyFileId,
        WishRecordSource source,
        WishStatus status,
        long recordedBy,
        Instant recordedAt) {}
