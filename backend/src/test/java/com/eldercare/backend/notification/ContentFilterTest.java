package com.eldercare.backend.notification;

import static com.eldercare.backend.common.notification.InformationType.CONTRACT;
import static com.eldercare.backend.common.notification.InformationType.COSTS;
import static com.eldercare.backend.common.notification.InformationType.GENERAL;
import static com.eldercare.backend.common.notification.InformationType.HEALTH;
import static com.eldercare.backend.common.notification.InformationType.VISITS;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.notification.internal.recipient.domain.ContentFilter;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * FR-013, DBR-03, Q-112 ({@code LocPhanNoiDung}; research B-04): người thân chỉ thấy phần nội dung được phép. "Chung" luôn;
 * sức khỏe cần quyền xem sức khỏe và bản đồng ý bao gồm người đó; chi phí, thăm nom theo quyền; hợp đồng chỉ người đại
 * diện; người thân không có quan hệ với người cao tuổi chỉ phần chung.
 */
class ContentFilterTest {

    private static final Set<InformationType> ALL = EnumSet.allOf(InformationType.class);

    @Test
    void generalAlways_othersByRight() {
        assertThat(ContentFilter.visibleParts(ALL, Set.of(), false, true, true)).containsExactly(GENERAL);
        assertThat(ContentFilter.visibleParts(ALL, Set.of(FamilyRight.VIEW_COSTS), false, true, true))
                .containsExactlyInAnyOrder(GENERAL, COSTS);
        assertThat(ContentFilter.visibleParts(ALL, Set.of(FamilyRight.REGISTER_VISITS), false, true, true))
                .containsExactlyInAnyOrder(GENERAL, VISITS);
        assertThat(ContentFilter.visibleParts(ALL, Set.of(), true, true, true))
                .containsExactlyInAnyOrder(GENERAL, CONTRACT);
    }

    @Test
    void DBR_03_health_needsRightAndConsent() {
        Set<FamilyRight> health = Set.of(FamilyRight.VIEW_HEALTH);

        assertThat(ContentFilter.visibleParts(ALL, health, false, true, true)).contains(HEALTH);
        assertThat(ContentFilter.visibleParts(ALL, health, false, true, false)).doesNotContain(HEALTH);
        assertThat(ContentFilter.visibleParts(ALL, Set.of(), false, true, true)).doesNotContain(HEALTH);
    }

    @Test
    void Q_112_noRelationship_generalOnly() {
        assertThat(ContentFilter.visibleParts(ALL, EnumSet.allOf(FamilyRight.class), true, false, true))
                .containsExactly(GENERAL);
    }

    @Test
    void onlyPartsPresentInRequest() {
        assertThat(ContentFilter.visibleParts(Set.of(HEALTH), Set.of(FamilyRight.VIEW_COSTS), false, true, true))
                .isEmpty();
        assertThat(ContentFilter.visibleParts(Set.of(GENERAL, HEALTH), Set.of(), false, true, true))
                .containsExactly(GENERAL);
    }
}
