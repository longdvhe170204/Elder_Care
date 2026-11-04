package com.eldercare.backend.family.web.response;

import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import io.swagger.v3.oas.annotations.media.Schema;

/** Hồ sơ người thân (api-012 {@code FamilyMember}). */
public record FamilyMemberResponse(
        long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fullName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String phoneNumber,
        String idDocumentType,
        String idDocumentNumber,
        String address,
        int version) {

    public static FamilyMemberResponse of(FamilyMember m) {
        return new FamilyMemberResponse(
                m.id(),
                m.fullName(),
                m.phoneNumber(),
                m.idDocumentType(),
                m.idDocumentNumber(),
                m.address(),
                m.version());
    }
}
