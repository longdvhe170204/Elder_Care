package com.eldercare.backend.support.fake.administration;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;
import com.eldercare.backend.common.notification.FamilyRight;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Bản giả {@link FakeFamilyRelationships}: loại quan hệ Hiệu lực giữa người thân và người cao tuổi (012 FR-002). */
class FakeFamilyRelationshipsTest {

    private final FakeFamilyRelationships family = new FakeFamilyRelationships();

    @Test
    void relationship_isTheTypeOfTheActiveRelationship() {
        long id = family.relate(1, 10, FamilyRelationshipType.CHILD);
        family.relate(1, 20, FamilyRelationshipType.GRANDCHILD);

        assertThat(family.relationship(1, 10)).contains(FamilyRelationshipType.CHILD);
        assertThat(family.relationship(1, 20)).contains(FamilyRelationshipType.GRANDCHILD);
        assertThat(family.relationship(1, 30)).isEmpty();
        assertThat(family.relationship(2, 10)).isEmpty();

        family.end(id);
        assertThat(family.relationship(1, 10)).isEmpty();
    }

    @Test
    void rightEnabled_byDefault_untilDisabledForThatResident() {
        family.relate(4, 10);
        family.relate(4, 20);

        family.disable(4, 10, FamilyRight.VIEW_HEALTH);

        assertThat(family.rightEnabled(4, 10, FamilyRight.VIEW_HEALTH, Instant.EPOCH))
                .isFalse();
        assertThat(family.rightEnabled(4, 10, FamilyRight.VIEW_COSTS, Instant.EPOCH))
                .isTrue();
        assertThat(family.rightEnabled(4, 20, FamilyRight.VIEW_HEALTH, Instant.EPOCH))
                .isTrue();
    }

    @Test
    void relateWithoutType_isOther() {
        family.relate(3, 10);

        assertThat(family.relationship(3, 10)).contains(FamilyRelationshipType.OTHER);
    }
}
