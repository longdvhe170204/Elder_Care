package com.cambae.backend.m09.credential.web.request;

import com.cambae.backend.m09.credential.internal.domain.CredentialInput;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;

/** Các trường ghi được trong schema Credential. */
public record CredentialInputBody(
        @NotNull Long credentialTypeId,
        String credentialNumber,
        String issuingAuthority,
        Set<String> practiceScopes,
        @NotNull LocalDate issueDate,
        LocalDate expiryDate,
        Long scanFileId) {
    public CredentialInput input() {
        return new CredentialInput(
                credentialTypeId,
                credentialNumber,
                issuingAuthority,
                practiceScopes,
                issueDate,
                expiryDate,
                scanFileId);
    }
}
