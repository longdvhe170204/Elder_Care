package com.cambae.backend.m01.internal.wish.domain;

import com.cambae.backend.m01.api.dto.HealthInsurance;
import com.cambae.backend.m01.api.dto.OpinionSource;
import com.cambae.backend.m01.api.dto.PreferredHospital;
import com.cambae.backend.m01.api.dto.TreatmentEscort;
import java.time.Instant;
import java.util.List;

/** Một phiên bản phiếu nguyện vọng khi điều trị (data-model mục 6b; FR-023j). */
public record TreatmentWish(
        long id,
        long residentId,
        List<PreferredHospital> preferredHospitals,
        HealthInsurance healthInsurance,
        boolean askFamilyFirst,
        List<Long> notifyFamilyMemberIds,
        TreatmentEscort accompaniedBy,
        String note,
        OpinionSource respondent,
        List<RecordedOpinion> otherOpinions,
        Long signedCopyFileId,
        TreatmentWishStatus status,
        long recordedBy,
        Instant recordedAt) {}
