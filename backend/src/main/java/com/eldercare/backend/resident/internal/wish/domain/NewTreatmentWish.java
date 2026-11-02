package com.eldercare.backend.resident.internal.wish.domain;

import com.eldercare.backend.resident.api.dto.HealthInsurance;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.PreferredHospital;
import com.eldercare.backend.resident.api.dto.TreatmentEscort;
import java.time.Instant;
import java.util.List;

/** Dữ liệu một phiên bản mới của phiếu nguyện vọng khi điều trị. */
public record NewTreatmentWish(
        long residentId,
        List<PreferredHospital> preferredHospitals,
        HealthInsurance healthInsurance,
        boolean askFamilyFirst,
        List<Long> notifyFamilyMemberIds,
        TreatmentEscort accompaniedBy,
        String note,
        OpinionSource respondent,
        Long signedCopyFileId,
        long recordedBy,
        Instant recordedAt) {}
