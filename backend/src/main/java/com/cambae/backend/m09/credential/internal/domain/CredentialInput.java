package com.cambae.backend.m09.credential.internal.domain;

import java.time.LocalDate;
import java.util.Set;

/** Dữ liệu ghi của schema Credential; không nhận các trường trạng thái chỉ đọc. */
public record CredentialInput(
        long credentialTypeId,
        String credentialNumber,
        String issuingAuthority,
        Set<String> practiceScopes,
        LocalDate issueDate,
        LocalDate expiryDate,
        Long scanFileId) {
    public CredentialInput {
        practiceScopes = practiceScopes == null ? Set.of() : Set.copyOf(practiceScopes);
    }
}
