package com.eldercare.backend.family.web.response;

import com.eldercare.backend.family.internal.relationship.service.FamilyMemberService.FamilyMemberDetail;
import java.util.List;

/** Hồ sơ người thân và các quan hệ (GET {@code /family-members/{id}}; FR-001, FR-002). */
public record FamilyMemberDetailResponse(FamilyMemberResponse familyMember, List<Item> relationships) {

    /** Quan hệ kèm người cao tuổi. */
    public record Item(long residentId, RelationshipResponse relationship) {}

    public static FamilyMemberDetailResponse of(FamilyMemberDetail detail) {
        return new FamilyMemberDetailResponse(
                FamilyMemberResponse.of(detail.familyMember()),
                detail.relationships().stream()
                        .map(r -> new Item(r.residentId(), RelationshipResponse.of(r, detail.familyMember())))
                        .toList());
    }
}
