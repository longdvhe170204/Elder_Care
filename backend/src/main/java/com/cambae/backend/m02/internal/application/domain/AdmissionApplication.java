package com.cambae.backend.m02.internal.application.domain;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import java.time.Instant;
import java.time.LocalDate;

/** Lượt đăng ký tiếp nhận (data-model mục 3, bảng {@code admission_application}). */
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
