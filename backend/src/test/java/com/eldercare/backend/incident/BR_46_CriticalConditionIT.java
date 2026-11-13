package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.incident.api.events.CriticalFlagRecorded;
import com.eldercare.backend.incident.api.events.TemporaryCriticalFlagConfirmationRequested;
import com.eldercare.backend.incident.api.events.WishReconfirmationRequested;
import com.eldercare.backend.resident.api.dto.WishChoice;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * BR-M05-15, FR-050b, FR-050d, FR-050e (US11 #1 → #3, #5): ghi dấu tạo cảnh báo Khẩn cấp không gộp kèm thông báo đội
 * ngũ, người thân (nội dung tối thiểu, mã Q-354) và yêu cầu gọi xác nhận lại; dấu tạm của Điều dưỡng chỉ khi không có
 * Bác sĩ trực; đóng cảnh báo cần kết quả xác nhận lại và lựa chọn.
 */
@RecordApplicationEvents
class BR_46_CriticalConditionIT extends CriticalCareTestBase {

    @Autowired
    ApplicationEvents events;

    @Test
    void FR_050b_flagNotifiesTeamFamilyAndRequestsReconfirmation() throws Exception {
        activeWish(WishChoice.RETURN_HOME);
        long alertId = flag(doctor).alertId();

        var recorded = events.stream(CriticalFlagRecorded.class).toList();
        assertThat(recorded).singleElement().satisfies(event -> {
            assertThat(event.alertId()).isEqualTo(alertId);
            assertThat(event.type()).isEqualTo("OFFICIAL");
        });
        var requests = recorded.getFirst().notificationRequests();
        var team = request(requests, null);
        assertThat(team.recipientGroups())
                .hasAtLeastOneElementOfType(RecipientGroup.OnDutyDoctor.class)
                .hasAtLeastOneElementOfType(RecipientGroup.AssignedNurse.class)
                .hasAtLeastOneElementOfType(RecipientGroup.FloorManager.class)
                .contains(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        var primary = request(requests, Q354Case.PRIMARY_CONTACT_EMERGENCY);
        assertThat(primary.recipientGroups()).containsExactly(new RecipientGroup.PrimaryContact(resident));
        var representative = request(requests, Q354Case.WISH_RECONFIRMATION);
        assertThat(representative.recipientGroups()).containsExactly(new RecipientGroup.Representative(resident));
        for (var family : List.of(primary, representative)) {
            assertThat(family.informationSubject()).isPresent();
            assertThat(family.content())
                    .extracting(ContentPart::content)
                    .noneMatch(text -> text.contains("nguyện vọng"));
            assertThat(family.displayParams()).doesNotContainKey("endOfLifeWish");
        }

        assertThat(events.stream(WishReconfirmationRequested.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.alertId()).isEqualTo(alertId);
                    var groups = event.notificationRequests().getFirst().recipientGroups();
                    assertThat(groups)
                            .hasAtLeastOneElementOfType(RecipientGroup.OnDutyDoctor.class)
                            .hasAtLeastOneElementOfType(RecipientGroup.AssignedNurse.class);
                    assertThat(event.notificationRequests()
                                    .getFirst()
                                    .displayParams()
                                    .get("callOrder"))
                            .isEqualTo(List.of(representativeId));
                });
        assertThat(events.stream(TemporaryCriticalFlagConfirmationRequested.class))
                .isEmpty();
    }

    private static NotificationRequest request(List<NotificationRequest> requests, Q354Case q354) {
        return requests.stream()
                .filter(r -> r.q354Case().equals(Optional.ofNullable(q354)))
                .findFirst()
                .orElseThrow();
    }
}
