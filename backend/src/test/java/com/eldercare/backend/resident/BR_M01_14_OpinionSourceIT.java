package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.OpinionSourceApi;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.OpinionSourceType;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationships;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 001 T012a (FR-023g, BR-M01-14; research H-16): nguồn ý kiến dùng chung. Người thân phải có quan hệ Hiệu lực với người
 * cao tuổi lúc gọi (bản giả {@code FamilyRelationshipApi} của 012); loại khác thì {@code familyMemberId} phải rỗng; sai
 * thì 422 {@code INVALID_OPINION_SOURCE}.
 */
@Import(FakeM15Config.class)
class BR_M01_14_OpinionSourceIT extends MySqlIntegrationTestBase {

    @Autowired
    OpinionSourceApi opinionSources;

    @Autowired
    FakeFamilyRelationships family;

    @Test
    void BR_M01_14_familyMemberWithActiveRelationship_isAccepted() {
        family.relate(41, 100);

        assertThatCode(() -> opinionSources.validate(100, new OpinionSource(OpinionSourceType.FAMILY_MEMBER, 41L)))
                .doesNotThrowAnyException();
    }

    @Test
    void BR_M01_14_familyMemberWithoutRelationshipOrEnded_isRejected() {
        long relationship = family.relate(42, 100);
        family.end(relationship);

        assertInvalid(100, new OpinionSource(OpinionSourceType.FAMILY_MEMBER, 42L));
        assertInvalid(100, new OpinionSource(OpinionSourceType.FAMILY_MEMBER, 43L));
        assertInvalid(101, new OpinionSource(OpinionSourceType.FAMILY_MEMBER, 41L));
    }

    @Test
    void BR_M01_14_familyMemberNeedsId_otherTypesMustNotHaveOne() {
        assertInvalid(100, new OpinionSource(OpinionSourceType.FAMILY_MEMBER, null));
        assertInvalid(100, new OpinionSource(OpinionSourceType.RESIDENT, 41L));
        assertInvalid(100, new OpinionSource(OpinionSourceType.OBSERVING_STAFF, 41L));
        assertInvalid(100, new OpinionSource(null, null));
        assertInvalid(100, null);
    }

    @Test
    void BR_M01_14_residentAndObservingStaff_acceptedWithoutFamilyMember() {
        assertThatCode(() -> opinionSources.validate(100, new OpinionSource(OpinionSourceType.RESIDENT, null)))
                .doesNotThrowAnyException();
        assertThatCode(() -> opinionSources.validate(100, new OpinionSource(OpinionSourceType.OBSERVING_STAFF, null)))
                .doesNotThrowAnyException();
    }

    private void assertInvalid(long residentId, OpinionSource source) {
        assertThatThrownBy(() -> opinionSources.validate(residentId, source))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("INVALID_OPINION_SOURCE");
                    assertThat(e.status().value()).isEqualTo(422);
                });
    }
}
