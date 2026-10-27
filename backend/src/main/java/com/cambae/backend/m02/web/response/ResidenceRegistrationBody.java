package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.internal.registration.domain.RegistrationStatus;
import com.cambae.backend.m02.internal.registration.domain.RegistrationType;
import com.cambae.backend.m02.internal.registration.domain.ResidenceRegistration;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

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
        boolean localPermanentResident) {

    public static ResidenceRegistrationBody of(ResidenceRegistration registration) {
        return new ResidenceRegistrationBody(
                registration.id(),
                registration.residentId(),
                registration.type(),
                registration.renewal(),
                registration.dueAt(),
                registration.status(),
                registration.expiryDate(),
                registration.localPermanentResident());
    }
}
