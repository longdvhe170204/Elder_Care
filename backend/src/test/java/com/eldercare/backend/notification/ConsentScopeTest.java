package com.eldercare.backend.notification;

import static com.eldercare.backend.common.notification.InformationSubject.ABOUT_FAMILY_MATTER;
import static com.eldercare.backend.common.notification.InformationSubject.ABOUT_RESIDENT;
import static com.eldercare.backend.common.notification.NotificationLevel.LOW;
import static com.eldercare.backend.common.notification.NotificationLevel.MEDIUM;
import static com.eldercare.backend.common.notification.NotificationLevel.URGENT;
import static com.eldercare.backend.notification.internal.recipient.domain.ConsentScope.Decision.MISSING_CASE;
import static com.eldercare.backend.notification.internal.recipient.domain.ConsentScope.Decision.SEND;
import static com.eldercare.backend.notification.internal.recipient.domain.ConsentScope.Decision.SEND_WITH_CASE;
import static com.eldercare.backend.notification.internal.recipient.domain.ConsentScope.Decision.SKIP;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.notification.internal.recipient.domain.ConsentScope;
import org.junit.jupiter.api.Test;

/**
 * FR-001a, FR-014, FR-014a; Q-353, Q-354 ({@code XetPhamViDongY}; research B-04): bảng đối tượng thông tin × trong, ngoài
 * phạm vi "nhận thông báo" của bản đồng ý × có, không mã trường hợp × mức.
 */
class ConsentScopeTest {

    @Test
    void aboutFamilyMatter_consentNotConsidered() {
        for (NotificationLevel level : NotificationLevel.values()) {
            assertThat(ConsentScope.decide(ABOUT_FAMILY_MATTER, false, false, level))
                    .isEqualTo(SEND);
        }
    }

    @Test
    void aboutResident_inScope_sendsWithoutCase() {
        for (NotificationLevel level : NotificationLevel.values()) {
            assertThat(ConsentScope.decide(ABOUT_RESIDENT, true, false, level)).isEqualTo(SEND);
            assertThat(ConsentScope.decide(ABOUT_RESIDENT, true, true, level)).isEqualTo(SEND);
        }
    }

    @Test
    void aboutResident_outOfScope_withCase_sendsAndRecordsCase() {
        for (NotificationLevel level : NotificationLevel.values()) {
            assertThat(ConsentScope.decide(ABOUT_RESIDENT, false, true, level)).isEqualTo(SEND_WITH_CASE);
        }
    }

    @Test
    void aboutResident_outOfScope_withoutCase_skipsOrMissingCaseWhenUrgent() {
        assertThat(ConsentScope.decide(ABOUT_RESIDENT, false, false, LOW)).isEqualTo(SKIP);
        assertThat(ConsentScope.decide(ABOUT_RESIDENT, false, false, MEDIUM)).isEqualTo(SKIP);
        assertThat(ConsentScope.decide(ABOUT_RESIDENT, false, false, URGENT)).isEqualTo(MISSING_CASE);
    }

    @Test
    void missingSubject_treatedAsAboutResident() {
        assertThat(ConsentScope.decide(null, false, false, MEDIUM)).isEqualTo(SKIP);
        assertThat(ConsentScope.decide(null, true, false, MEDIUM)).isEqualTo(SEND);
    }
}
