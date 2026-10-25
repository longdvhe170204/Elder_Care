package com.cambae.backend.m01.internal.wish.domain;

import com.cambae.backend.m01.api.dto.CourtDecisionRef;
import com.cambae.backend.m01.api.dto.RepresentativeSelection;
import com.cambae.backend.m01.api.dto.WishChoice;
import com.cambae.backend.m01.api.dto.WishRecordSource;
import com.cambae.backend.m01.api.dto.WishSignerType;
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
