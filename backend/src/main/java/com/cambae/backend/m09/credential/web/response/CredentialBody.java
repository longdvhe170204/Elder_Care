package com.cambae.backend.m09.credential.web.response;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.credential.internal.domain.Credential;
import com.cambae.backend.m09.credential.internal.domain.CredentialValidity;
import com.cambae.backend.m15.account.api.*;
import com.cambae.backend.m15.account.api.dto.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Schema Credential; không để số giấy phép hoặc bản scan đi theo nhóm công khai. */
@Schema(name = "Credential")
public record CredentialBody(
        long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long credentialTypeId,
        @FieldGroup(InfoGroup.STAFF_RESTRICTED) String credentialNumber,
        String issuingAuthority,
        Set<String> practiceScopes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate issueDate,
        LocalDate expiryDate,
        @FieldGroup(InfoGroup.STAFF_RESTRICTED) Long scanFileId,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Long replacesId,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) LocalDate revokedFrom,

        @Schema(
                accessMode = Schema.AccessMode.READ_ONLY,
                allowableValues = {"VALID", "EXPIRED", "REVOKED", "REPLACED", "NOT_YET_ISSUED"})
        String validityStatus,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY) List<String> hiddenFields)
        implements VisibilityTarget {
    public Target visibilityTarget() {
        return new Target("M09.CREDENTIAL", Long.toString(id), null, null);
    }

    public static CredentialBody of(Credential c, BusinessTime time) {
        return new CredentialBody(
                c.id(),
                c.credentialTypeId(),
                c.credentialNumber(),
                c.issuingAuthority(),
                c.practiceScopes(),
                c.issueDate(),
                c.expiryDate(),
                c.scanFileId(),
                c.replacesId(),
                c.revokedFrom(),
                CredentialValidity.status(
                                c.issueDate(), c.expiryDate(), c.revokedFrom(), c.replaced(), time.now(), time)
                        .name(),
                List.of());
    }
}
