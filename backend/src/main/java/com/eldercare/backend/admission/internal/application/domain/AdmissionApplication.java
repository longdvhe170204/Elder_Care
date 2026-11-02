package com.eldercare.backend.admission.internal.application.domain;

import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.time.Instant;
import java.time.LocalDate;

/** Lượt đăng ký tiếp nhận (data-model mục 3, dòng {@code APPLICATION} của {@code admission_record}). */
public record AdmissionApplication(
        long id,
        long residentId,
        LocalDate applicationDate,
        String contactPerson,
        String needs,
        ResidenceType desiredResidenceType,
        CareLevel expectedCareLevel,
        LocalDate desiredAdmissionDate,
        String note,
        ApplicationStatus status,
        Long opinionAtAdmissionId,
        int version,
        long createdBy,
        Instant createdAt) {

    /** Nội dung người dùng nhập, dùng khi lập và khi cập nhật. */
    public record Content(
            LocalDate applicationDate,
            String contactPerson,
            String needs,
            ResidenceType desiredResidenceType,
            CareLevel expectedCareLevel,
            LocalDate desiredAdmissionDate,
            String note) {}

    public Content content() {
        return new Content(
                applicationDate,
                contactPerson,
                needs,
                desiredResidenceType,
                expectedCareLevel,
                desiredAdmissionDate,
                note);
    }
}
