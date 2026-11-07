package com.eldercare.backend.family.internal.portal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.family.internal.portal.domain.HealthAccess.Decision;
import org.junit.jupiter.api.Test;

/**
 * 012 T026 ({@code SC_002_SucKhoeTest}; FR-011, FR-046 (2), FR-048, DBR-03, SC-002): phần sức khỏe chỉ mở khi quyền xem sức
 * khỏe đang bật và có bản đồng ý Hiệu lực bao gồm người thân; tách hai lý do từ chối; chín phần của cổng ứng với mã phần
 * của nhật ký lượt xem (data-model mục 7).
 */
class SC_002_HealthTest {

    @Test
    void SC_002_onlyEnabledRightWithConsent_opensHealth() {
        assertThat(HealthAccess.decide(false, false)).isEqualTo(Decision.NO_HEALTH_VIEW_RIGHT);
        assertThat(HealthAccess.decide(false, true)).isEqualTo(Decision.NO_HEALTH_VIEW_RIGHT);
        assertThat(HealthAccess.decide(true, false)).isEqualTo(Decision.RIGHT_NOT_EFFECTIVE);
        assertThat(HealthAccess.decide(true, true)).isEqualTo(Decision.ALLOWED);
    }

    @Test
    void FR_048_everyPortalSection_hasALogCode() {
        assertThat(HealthSection.values()).hasSize(9);
        assertThat(HealthSection.fromPath("allergies-conditions")).contains(HealthSection.ALLERGIES_CONDITIONS);
        assertThat(HealthSection.fromPath("medical-appointments")).contains(HealthSection.MEDICAL_APPOINTMENTS);
        assertThat(HealthSection.fromPath("history")).isEmpty();
        assertThat(HealthSection.ALLERGIES_CONDITIONS.path()).isEqualTo("allergies-conditions");
    }
}
