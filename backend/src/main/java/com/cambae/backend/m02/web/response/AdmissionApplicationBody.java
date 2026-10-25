package com.cambae.backend.m02.web.response;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.internal.application.domain.AdmissionApplication;
import com.cambae.backend.m02.internal.application.domain.AdmissionOpinion;
import com.cambae.backend.m02.internal.application.domain.ApplicationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.function.LongFunction;

/**
 * Lượt đăng ký tiếp nhận (contracts/api-004.yaml, schema {@code AdmissionApplication}). {@code admissionOpinion} (ý
 * kiến hiện hành rồi lịch sử) chỉ có ở phản hồi một lượt; danh sách không kèm để tránh truy vấn từng dòng.
 * {@code waitingListEntryId} có ở user story hồ sơ chờ.
 */
@Schema(name = "AdmissionApplication")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdmissionApplicationBody(
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
        Long waitingListEntryId,
        List<AdmissionOpinionBody> admissionOpinion,
        Long opinionAtAdmissionId) {

    public static AdmissionApplicationBody of(AdmissionApplication application) {
        return of(application, null, null);
    }

    public static AdmissionApplicationBody of(
            AdmissionApplication application, List<AdmissionOpinion> opinions, LongFunction<String> names) {
        return new AdmissionApplicationBody(
                application.id(),
                application.residentId(),
                application.applicationDate(),
                application.contactPerson(),
                application.needs(),
                application.desiredResidenceType(),
                application.expectedCareLevel(),
                application.desiredAdmissionDate(),
                application.note(),
                application.status(),
                null,
                opinions == null
                        ? null
                        : opinions.stream()
                                .map(opinion -> AdmissionOpinionBody.of(opinion, names))
                                .toList(),
                application.opinionAtAdmissionId());
    }
}
