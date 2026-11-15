package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.events.ContractOverdueEscalated;
import com.eldercare.backend.admission.api.events.DeathRecorded;
import com.eldercare.backend.admission.api.events.DepositNotSatisfiedAfterAdmission;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * 004 T004, research T-12: mức và người nhận của các sự kiện gửi thông báo (009 cài một listener cho
 * {@link HasNotificationRequests}); sự kiện dành cho module khác không gửi thông báo.
 */
class M02EventsTest {

    private static final Instant AT = Instant.parse("2030-03-04T01:00:00Z");

    private static NotificationRequest single(HasNotificationRequests event) {
        assertThat(event.notificationRequests()).hasSize(1);
        NotificationRequest request = event.notificationRequests().get(0);
        assertThat(request.sourceModule()).isEqualTo("M02");
        assertThat(request.eventKey()).isNotBlank();
        return request;
    }

    private static void assertRecipient(NotificationRequest request, Role role) {
        assertThat(request.recipientGroups()).containsExactly(new RecipientGroup.FacilityRole(role));
    }

    @Test
    void FR_029_overdueEscalated_notifiesManagerAtMediumLevel() {
        NotificationRequest request = single(new ContractOverdueEscalated(2L, 11L, 15, AT));

        assertThat(request.level()).contains(NotificationLevel.MEDIUM);
        assertRecipient(request, Role.NURSING_HOME_MANAGER);
    }

    @Test
    void FR_032_depositNotSatisfiedAfterAdmission_notifiesManagerAtMediumLevel() {
        NotificationRequest request = single(new DepositNotSatisfiedAfterAdmission(2L, 11L, AT));

        assertThat(request.level()).contains(NotificationLevel.MEDIUM);
        assertRecipient(request, Role.NURSING_HOME_MANAGER);
    }

    @Test
    void eventsForOtherModules_doNotSendNotifications() {
        assertThat(HasNotificationRequests.class.isAssignableFrom(DeathRecorded.class))
                .isFalse();
    }
}
