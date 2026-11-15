package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.admission.internal.application.domain.AdmissionApplication;
import com.eldercare.backend.admission.internal.application.domain.AdmissionOpinion;
import com.eldercare.backend.admission.internal.application.domain.ApplicationStatus;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.function.LongFunction;

/**
 * Lượt đăng ký tiếp nhận (contracts/api-004.yaml, schema {@code AdmissionApplication}). {@code admissionOpinion} (ý
 * kiến hiện hành rồi lịch sử) và {@code waitingListEntryId} chỉ có ở phản hồi một lượt; danh sách không kèm để tránh truy
 * vấn từng dòng.
 */
@Schema(
        name = "AdmissionApplication",
        requiredProperties = {"residentId", "applicationDate", "desiredResidenceType", "expectedCareLevel"})
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
        return of(application, null, null, null);
    }

    public static AdmissionApplicationBody of(
            AdmissionApplication application, List<AdmissionOpinion> opinions, LongFunction<String> names) {
        return of(application, opinions, names, null);
    }

    public static AdmissionApplicationBody of(
            AdmissionApplication application,
            List<AdmissionOpinion> opinions,
            LongFunction<String> names,
            Long waitingListEntryId) {
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
                waitingListEntryId,
                opinions == null
                        ? null
                        : opinions.stream()
                                .map(opinion -> AdmissionOpinionBody.of(opinion, names))
                                .toList(),
                application.opinionAtAdmissionId());
    }
}
