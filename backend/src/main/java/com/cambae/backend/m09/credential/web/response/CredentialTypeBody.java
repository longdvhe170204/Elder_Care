package com.cambae.backend.m09.credential.web.response;

import com.cambae.backend.m09.credential.internal.domain.CredentialType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

/** Schema CredentialType, không xuất code kỹ thuật hoặc version không có trong hợp đồng. */
@Schema(name = "CredentialType")
public record CredentialTypeBody(
        long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"PROFESSIONAL_LICENSE", "TRAINING"})
        String group,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasExpiry,
        Set<String> requiredForRoles,
        boolean hasPracticeScopes,
        boolean active) {
    public static CredentialTypeBody of(CredentialType t) {
        return new CredentialTypeBody(
                t.id(), t.name(), t.group(), t.hasExpiry(), t.requiredForRoles(), t.hasPracticeScopes(), t.active());
    }
}
