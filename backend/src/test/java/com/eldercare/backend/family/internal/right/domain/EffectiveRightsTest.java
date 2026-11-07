package com.eldercare.backend.family.internal.right.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.support.TableRows;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** 012 T005 (research F-04; FR-011, DBR-03): quyền có tác dụng. */
class EffectiveRightsTest {

    @Test
    void DBR_03_viewHealth_effectiveOnlyWhenConsentIncludesFamilyMember() {
        assertThat(EffectiveRights.effective(FamilyRight.VIEW_HEALTH, true, true))
                .isTrue();
        assertThat(EffectiveRights.effective(FamilyRight.VIEW_HEALTH, true, false))
                .isFalse();
    }

    @Test
    void FR_011_enabledViewHealth_withoutConsent_isEnabledButNotEffective() {
        assertThat(EffectiveRights.enabledButNotEffective(FamilyRight.VIEW_HEALTH, true, false))
                .isTrue();
        assertThat(EffectiveRights.enabledButNotEffective(FamilyRight.VIEW_HEALTH, false, false))
                .isFalse();
        assertThat(EffectiveRights.enabledButNotEffective(FamilyRight.VIEW_HEALTH, true, true))
                .isFalse();
    }

    @Test
    void FR_007_otherRights_effectiveWhenEnabled_regardlessOfConsent() {
        TableRows.each(Arrays.stream(FamilyRight.values()).filter(right -> right != FamilyRight.VIEW_HEALTH), right -> {
            assertThat(EffectiveRights.effective(right, true, false)).isTrue();
            assertThat(EffectiveRights.effective(right, false, true)).isFalse();
        });
    }

    @Test
    void FR_007_disabledRight_isNeverEffective() {
        TableRows.each(
                Arrays.stream(FamilyRight.values()),
                right -> assertThat(EffectiveRights.effective(right, false, true))
                        .isFalse());
    }
}
