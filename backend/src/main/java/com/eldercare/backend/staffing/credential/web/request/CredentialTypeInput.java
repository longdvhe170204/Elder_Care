package com.eldercare.backend.staffing.credential.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/** Nội dung schema CredentialType. */
public record CredentialTypeInput(
        @NotBlank String name,
        @NotBlank String group,
        @NotNull Boolean hasExpiry,
        Set<String> requiredForRoles,
        Boolean hasPracticeScopes,
        Boolean active) {}
