package com.cambae.backend.m15.account.web.response;

import com.cambae.backend.m15.account.internal.legal.ProfessionalActivity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "ProfessionalActivity")
public record ProfessionalActivityBody(
        Long id,
        String code,
        String name,
        String function,
        String requiredLicenseType,
        List<Long> requiredPracticeScopes,
        Boolean requiresFacilityScope,
        Boolean isDefault,
        Status status,
        Integer version) {

    public static ProfessionalActivityBody of(ProfessionalActivity a) {
        return new ProfessionalActivityBody(
                a.id(),
                a.code(),
                a.name(),
                a.function().name(),
                a.requiredLicenseType(),
                a.practiceScopes().stream().sorted().toList(),
                a.requiresFacilityScope(),
                a.isDefault(),
                a.active() ? Status.ACTIVE : Status.INACTIVE,
                (int) a.version());
    }
}
