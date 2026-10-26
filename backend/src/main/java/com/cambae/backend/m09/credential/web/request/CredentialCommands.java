package com.cambae.backend.m09.credential.web.request;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m09.credential.internal.domain.CredentialInput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;

public final class CredentialCommands {
    private CredentialCommands() {}

    public record Revoke(
            @NotNull LocalDate effectiveFrom,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {}

    public record CorrectionReason(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {}

    public record Correct(
            @NotNull Long credentialTypeId,
            String credentialNumber,
            String issuingAuthority,
            Set<String> practiceScopes,
            @NotNull LocalDate issueDate,
            LocalDate expiryDate,
            Long scanFileId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {
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
}
