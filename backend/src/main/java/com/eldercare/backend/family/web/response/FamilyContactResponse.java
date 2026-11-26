package com.eldercare.backend.family.web.response;

import com.eldercare.backend.family.internal.relationship.domain.FamilyContact;
import io.swagger.v3.oas.annotations.media.Schema;

/** Một người thân trong danh sách liên hệ (api-012 {@code FamilyContact}; Q-448, FR-077). */
@Schema(name = "FamilyContact", description = "Người thân có quan hệ Hiệu lực, chỉ các trường để liên hệ (Q-448)")
public record FamilyContactResponse(
        long familyMemberId,
        String fullName,

        @Schema(allowableValues = {"SPOUSE", "CHILD", "GRANDCHILD", "SIBLING", "OTHER"})
        String relationship,

        String phoneNumber,
        boolean representative,
        boolean primaryContact,
        int contactOrder) {

    public static FamilyContactResponse of(FamilyContact c) {
        return new FamilyContactResponse(
                c.familyMemberId(),
                c.fullName(),
                c.relationship().name(),
                c.phoneNumber(),
                c.representative(),
                c.primaryContact(),
                c.contactOrder());
    }
}
