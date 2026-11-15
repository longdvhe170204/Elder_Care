package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.belongings.api.events.BelongingNotice;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 013 T002, FR-032, research K-13 (009 FR-001a, Q-362): mọi yêu cầu thông báo của M12 tới người thân khai đối tượng
 * "về việc của chính người thân" và không mang mã trường hợp Q-354.
 */
class T002_BelongingNoticeTest {

    @Test
    void FR_032_everyFamilyRecipientGroup_requiresFamilyMatterSubject() {
        for (RecipientGroup family : List.of(
                new RecipientGroup.PrimaryContact(7L),
                new RecipientGroup.Representative(7L),
                new RecipientGroup.FamilyMember(9L, "BELONGING_GIVER"))) {
            assertThatThrownBy(() -> new BelongingNotice(List.of(request(family, Optional.empty(), null))))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BelongingNotice(
                            List.of(request(family, Optional.of(InformationSubject.ABOUT_RESIDENT), null))))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void K_13_familyRequest_neverCarriesQ354Case() {
        assertThatThrownBy(() -> new BelongingNotice(List.of(request(
                        new RecipientGroup.Representative(7L),
                        Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                        Q354Case.DEATH))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void staffOnlyRequest_needsNoSubject() {
        var notice = new BelongingNotice(
                List.of(request(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF), Optional.empty(), null)));

        assertThat(notice.notificationRequests()).hasSize(1);
    }

    private static NotificationRequest request(
            RecipientGroup recipients, Optional<InformationSubject> subject, Q354Case q354) {
        return new NotificationRequest(
                "M12",
                "BELONGING_LOST",
                SourceGroup.OTHER,
                new SourceRecord("M12.BELONGING", "1"),
                "BELONGING_LOST:1",
                Optional.of(NotificationLevel.MEDIUM),
                Optional.of(7L),
                Optional.empty(),
                List.of(recipients),
                List.of(new ContentPart(InformationType.GENERAL, "Điện thoại: báo thất lạc")),
                Optional.of("Điện thoại: báo thất lạc"),
                Optional.of("Đồ gửi"),
                Optional.empty(),
                false,
                Instant.parse("2026-11-05T03:00:00Z"),
                Map.of(),
                false,
                subject,
                Optional.ofNullable(q354));
    }
}
