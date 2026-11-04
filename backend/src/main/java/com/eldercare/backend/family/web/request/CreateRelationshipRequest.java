package com.eldercare.backend.family.web.request;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;
import com.eldercare.backend.family.internal.relationship.service.FamilyMemberService.CreateRelationship;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Thân lập quan hệ (api-012 {@code CreateRelationship}; FR-002, FR-005). */
@Schema(name = "CreateRelationship")
public record CreateRelationshipRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long familyMemberId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        FamilyRelationshipType relationship,

        Boolean primaryContact,
        Boolean setFirstRepresentative,
        Long evidenceFileId,
        String reason) {

    public CreateRelationship command() {
        return new CreateRelationship(
                familyMemberId,
                relationship,
                Boolean.TRUE.equals(primaryContact),
                Boolean.TRUE.equals(setFirstRepresentative),
                evidenceFileId,
                reason);
    }
}
