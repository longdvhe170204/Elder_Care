package com.eldercare.backend.family.internal.relationship.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Q-448, 012 FR-077 (test luồng BF-01 2026-11-26): danh sách liên hệ người thân chỉ gồm quan hệ Hiệu lực, sắp theo thứ
 * tự liên hệ, chỉ các trường để liên hệ (không quyền, giấy tờ, địa chỉ).
 */
class FamilyContactListTest {

    private static final Instant START = Instant.parse("2026-11-01T00:00:00Z");

    private static FamilyRelationship relationship(
            long id, long memberId, int order, boolean active, boolean representative, boolean primary) {
        return new FamilyRelationship(
                id,
                memberId,
                9L,
                FamilyRelationshipType.CHILD,
                representative,
                primary,
                order,
                Set.of(),
                active,
                START,
                active ? null : START.plusSeconds(60));
    }

    @Test
    void FR_077_onlyActiveRelationships_sortedByContactOrder_withContactFieldsOnly() {
        Map<Long, FamilyMember> members = Map.of(
                1L, new FamilyMember(1L, "Trần Thị Lan", "0912000222", "CCCD", "001180004321", "Hà Nội", 0),
                2L, new FamilyMember(2L, "Trần Văn Hùng", "0912000333", null, null, null, 0),
                3L, new FamilyMember(3L, "Nguyễn Văn Cũ", "0912000444", null, null, null, 0));

        List<FamilyContact> contacts = FamilyContact.listOf(
                List.of(
                        relationship(10L, 2L, 2, true, false, false),
                        relationship(11L, 3L, 1, false, true, true),
                        relationship(12L, 1L, 1, true, true, true)),
                members::get);

        assertThat(contacts)
                .containsExactly(
                        new FamilyContact(
                                1L, "Trần Thị Lan", FamilyRelationshipType.CHILD, "0912000222", true, true, 1),
                        new FamilyContact(
                                2L, "Trần Văn Hùng", FamilyRelationshipType.CHILD, "0912000333", false, false, 2));
    }
}
