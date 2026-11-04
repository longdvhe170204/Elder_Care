package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 012 T009 (FR-016, Q-249, Q-267; FR-069): quyền trên phiếu đăng ký có hiệu lực thì 009 gửi mức Nhẹ cho mọi người đại
 * diện Hiệu lực khác, trừ người lập và người xác nhận (người ký phiếu); loại thông tin "chung", về việc của chính người
 * thân (F-15).
 */
class Q249_OtherRepresentativesNotifiedIT extends FamilyCommandTestBase {

    @Test
    void Q_249_otherRepresentatives_areNotified_notTheSigner() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long rMember = member("Trần R");
        relate(a, rMember, true, true);
        long sMember = member("Trần S");
        fixtures.relate(sMember, a, 5, true, false);
        long n = relate(a, member("Trần N"), false, false);

        record(a, rMember, n);

        List<NotificationRequest> requests = q249(a);
        assertThat(requests).singleElement().satisfies(request -> {
            assertThat(request.level()).contains(NotificationLevel.LOW);
            assertThat(request.informationSubject()).contains(InformationSubject.ABOUT_FAMILY_MATTER);
            assertThat(request.recipientGroups())
                    .containsExactly(new RecipientGroup.FamilyMember(sMember, "REPRESENTATIVE"));
        });
    }

    @Test
    void Q_249_aSingleRepresentative_receivesNoExtraNotice() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long rMember = member("Lê R");
        relate(a, rMember, true, true);
        long n = relate(a, member("Lê N"), false, false);

        record(a, rMember, n);

        assertThat(q249(a)).isEmpty();
    }

    private void record(long residentId, long signer, long relationshipId) throws Exception {
        post(
                        "/api/v1/residents/" + residentId + "/family-registration-forms",
                        "{\"representativeId\": " + signer + ", \"scanFileId\": " + file()
                                + ", \"lines\": [{\"relationshipId\": " + relationshipId
                                + ", \"right\": \"REGISTER_VISITS\"}]}")
                .andExpect(status().isCreated());
    }

    private List<NotificationRequest> q249(long residentId) {
        return events.stream(HasNotificationRequests.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(r -> r.eventType().equals("RIGHT_CHANGE_EFFECTIVE_OTHER_REPRESENTATIVES"))
                .filter(r -> r.residentId().equals(Optional.of(residentId)))
                .toList();
    }
}
