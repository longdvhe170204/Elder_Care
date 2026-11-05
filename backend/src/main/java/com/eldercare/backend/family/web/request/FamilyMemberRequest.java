package com.eldercare.backend.family.web.request;

import com.eldercare.backend.family.internal.relationship.service.FamilyMemberService.FamilyMemberData;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Thân lập, sửa hồ sơ người thân (api-012 {@code FamilyMember}; FR-001). */
@Schema(name = "FamilyMember")
public record FamilyMemberRequest(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Long id,

        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String fullName,

        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String phoneNumber,

        String idDocumentType,
        String idDocumentNumber,
        String address,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Integer version) {

    public FamilyMemberData data() {
        return new FamilyMemberData(fullName.trim(), phoneNumber.trim(), idDocumentType, idDocumentNumber, address);
    }
}
