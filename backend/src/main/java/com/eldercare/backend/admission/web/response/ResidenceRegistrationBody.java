package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.admission.internal.registration.domain.RegistrationStatus;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationSubmission;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationType;
import com.eldercare.backend.admission.internal.registration.domain.ResidenceRegistration;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Khai báo tạm trú, lưu trú (contracts/api-018.yaml, schema {@code ResidenceRegistration}; FR-003). */
@Schema(name = "ResidenceRegistration")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResidenceRegistrationBody(
        long id,
        long residentId,
        RegistrationType type,
        boolean renewal,
        Instant dueAt,
        RegistrationStatus status,
        LocalDate expiryDate,
        Long evidenceFileId,
        boolean localPermanentResident,
        String basisNote,
        List<String> allowedCommands,
        List<RegistrationSubmissionBody> submissions) {

    public static ResidenceRegistrationBody of(
            ResidenceRegistration registration,
            List<String> allowedCommands,
            List<RegistrationSubmission> submissions) {
        return new ResidenceRegistrationBody(
                registration.id(),
                registration.residentId(),
                registration.type(),
                registration.renewal(),
                registration.dueAt(),
                registration.status(),
                registration.expiryDate(),
                registration.evidenceFileId(),
                registration.localPermanentResident(),
                registration.basisNote(),
                allowedCommands,
                submissions.stream().map(RegistrationSubmissionBody::of).toList());
    }
}
