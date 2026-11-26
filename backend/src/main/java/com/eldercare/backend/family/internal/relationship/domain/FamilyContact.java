package com.eldercare.backend.family.internal.relationship.domain;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;
import java.util.Comparator;
import java.util.List;
import java.util.function.LongFunction;

/**
 * Một dòng của danh sách liên hệ người thân (Q-448, 012 FR-077): chỉ trường để liên hệ, không quyền, giấy tờ, địa chỉ.
 */
public record FamilyContact(
        long familyMemberId,
        String fullName,
        FamilyRelationshipType relationship,
        String phoneNumber,
        boolean representative,
        boolean primaryContact,
        int contactOrder) {

    /** Quan hệ Hiệu lực của người cao tuổi, sắp theo thứ tự liên hệ. */
    public static List<FamilyContact> listOf(
            List<FamilyRelationship> relationships, LongFunction<FamilyMember> members) {
        return relationships.stream()
                .filter(FamilyRelationship::active)
                .sorted(Comparator.comparingInt(FamilyRelationship::contactOrder))
                .map(r -> {
                    FamilyMember member = members.apply(r.familyMemberId());
                    return new FamilyContact(
                            r.familyMemberId(),
                            member.fullName(),
                            r.relationship(),
                            member.phoneNumber(),
                            r.representative(),
                            r.primaryContact(),
                            r.contactOrder());
                })
                .toList();
    }
}
