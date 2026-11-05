package com.eldercare.backend.family.web.request;

import com.eldercare.backend.family.internal.stay.service.FamilyStayService.Registration;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Thân đăng ký ở lại (api-012 {@code FamilyStayRegistration}; FR-040). */
@Schema(name = "FamilyStayRegistration")
public record FamilyStayRegistrationRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long familyMemberId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long residentId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Instant expectedStartAt,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Instant expectedEndAt,

        @NotNull @Size(max = 100) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String location,

        @NotNull @Size(max = 1000) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String reason,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean mealRegistered) {

    public Registration registration() {
        return new Registration(
                familyMemberId, residentId, expectedStartAt, expectedEndAt, location, reason, mealRegistered);
    }
}
