package com.cambae.backend.m15.account.web.request;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m15.account.internal.legal.service.ProfessionalActivityService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

@Schema(name = "ProfessionalActivityInput")
public record ProfessionalActivityInput(
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String function,

        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String requiredLicenseType,

        List<Long> requiredPracticeScopes,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean requiresFacilityScope,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {

    public ProfessionalActivityService.Input input() {
        return new ProfessionalActivityService.Input(
                name,
                function,
                requiredLicenseType,
                requiredPracticeScopes == null ? Set.of() : Set.copyOf(requiredPracticeScopes),
                requiresFacilityScope);
    }
}
