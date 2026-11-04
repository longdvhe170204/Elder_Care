package com.eldercare.backend.support.fake.family;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.api.dto.FamilyMemberView;
import com.eldercare.backend.family.api.dto.IdDocumentView;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FakeFamilyRightsTest {

    private final FakeFamilyRights family = new FakeFamilyRights();
    private final Instant now = Instant.parse("2026-11-04T03:00:00Z");

    @Test
    void rolesRightsAndPickupList_perResident() {
        family.representative(1, 10)
                .primaryContact(2, 10, FamilyRight.PICKUP_ALLOWED)
                .relate(3, 10, FamilyRight.VIEW_COSTS)
                .relate(1, 20, FamilyRight.VIEW_HEALTH)
                .idDocument(2, "CCCD", "079000000002");

        assertThat(family.hasActiveRelationship(3, 10)).isTrue();
        assertThat(family.hasActiveRelationship(3, 20)).isFalse();
        assertThat(family.isRepresentative(1, 10, now)).isTrue();
        assertThat(family.isRepresentative(1, 20, now)).isFalse();
        assertThat(family.rightEffective(3, 10, FamilyRight.VIEW_COSTS, now)).isTrue();
        assertThat(family.rightEffective(1, 10, FamilyRight.VIEW_COSTS, now)).isFalse();
        assertThat(family.representatives(10))
                .extracting(FamilyMemberView::familyMemberId)
                .containsExactly(1L);
        assertThat(family.primaryContact(10))
                .map(FamilyMemberView::familyMemberId)
                .contains(2L);
        assertThat(family.allowedPickersAt(10, now))
                .extracting(FamilyMemberView::familyMemberId)
                .containsExactly(2L);
        assertThat(family.canViewBelongings(1, 10, now)).isTrue();
        assertThat(family.canViewBelongings(2, 10, now)).isTrue();
        assertThat(family.canViewBelongings(3, 10, now)).isFalse();
        assertThat(family.idDocument(2)).contains(new IdDocumentView("CCCD", "079000000002"));
        assertThat(family.idDocument(3)).isEmpty();

        family.reset();
        assertThat(family.hasActiveRelationship(1, 10)).isFalse();
    }
}
