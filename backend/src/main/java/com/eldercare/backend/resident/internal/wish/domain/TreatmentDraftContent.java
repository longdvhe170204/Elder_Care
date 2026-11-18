package com.eldercare.backend.resident.internal.wish.domain;

import com.eldercare.backend.resident.api.dto.HealthInsurance;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.PreferredHospital;
import com.eldercare.backend.resident.api.dto.TreatmentEscort;
import com.eldercare.backend.resident.api.dto.WishOpinion;
import java.util.List;

/** Nội dung bản Nháp tự khai của phiếu nguyện vọng khi điều trị (Q-412, FR-023n); mọi trường trừ người trả lời để trống được. */
public record TreatmentDraftContent(
        List<PreferredHospital> preferredHospitals,
        HealthInsurance healthInsurance,
        Boolean askFamilyFirst,
        List<Long> notifyFamilyMemberIds,
        TreatmentEscort accompaniedBy,
        String note,
        OpinionSource respondent,
        List<WishOpinion> otherOpinions,
        Long signedCopyFileId) {}
