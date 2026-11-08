package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.incident.api.events.AlertCreated;
import com.eldercare.backend.incident.api.events.AlertLevelRaised;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * T016a: cảnh báo tạo hoặc nâng mức phát sự kiện kèm nhóm người nhận theo FR-035, FR-047a (spec 007, research S-19);
 * người liên hệ chính và Quản lý viện không được báo ở bước này (Quản lý viện chỉ khi không có Bác sĩ trực, FR-047b).
 */
class FR_035_AlertCreatedRecipientsIT extends AlertRequestTestBase {
    private static final long PERFORMER = 7_001L;

    @Test
    void FR_035_mediumNotifiesAssignedNurseAndAddsSourcePerformer() {
        request("T016A:MEDIUM_PLAIN", IncidentLevel.MEDIUM, T0, 1);
        var low = created();
        assertThat(low.level()).isEqualTo("MEDIUM");
        assertThat(groups(low)).containsExactly(new RecipientGroup.AssignedNurse(resident, anySubstitution()));
        assertThat(level(low)).isEqualTo(NotificationLevel.MEDIUM);

        published.clear();
        transaction.executeWithoutResult(s -> service.requestAlert(new CreateAlertCommand(
                resident,
                null,
                "CHI_SO",
                "T016A:MEDIUM",
                IncidentLevel.MEDIUM,
                T0,
                "M06.VITAL_SIGN_RECORD",
                2L,
                null,
                null,
                null,
                PERFORMER)));
        var medium = created();
        assertThat(groups(medium))
                .containsExactly(
                        new RecipientGroup.AssignedNurse(resident, anySubstitution()),
                        new RecipientGroup.Staff(PERFORMER, "ALERT_SOURCE_PERFORMER"));
        assertThat(level(medium)).isEqualTo(NotificationLevel.MEDIUM);
    }

    @Test
    void FR_047a_criticalNotifiesFourGroupsWithoutPrimaryContactOrManagerAndRaiseNotifiesAgain() {
        request("T016A:RAISE", IncidentLevel.MEDIUM, T0, 3);
        published.clear();
        request("T016A:RAISE", IncidentLevel.CRITICAL, T0.plusSeconds(60), 4);
        var raised = published.stream()
                .filter(AlertLevelRaised.class::isInstance)
                .map(AlertLevelRaised.class::cast)
                .findFirst()
                .orElseThrow();
        var groups = groups(raised);
        assertThat(groups)
                .contains(
                        new RecipientGroup.AssignedNurse(resident, anySubstitution()),
                        new RecipientGroup.ShiftLead(floor, anySubstitution()),
                        new RecipientGroup.FloorManager(floor, anySubstitution()),
                        new RecipientGroup.OnDutyDoctor(anySubstitution()))
                .noneMatch(g -> g instanceof RecipientGroup.PrimaryContact)
                .doesNotContain(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        assertThat(level(raised)).isEqualTo(NotificationLevel.URGENT);
        assertThat(raised.notificationRequests().get(0).eventKey()).contains(":RAISED:CRITICAL");

        published.clear();
        request("T016A:RAISE", IncidentLevel.CRITICAL, T0.plusSeconds(120), 5);
        assertThat(published).noneMatch(AlertLevelRaised.class::isInstance);
    }

    private AlertCreated created() {
        return published.stream()
                .filter(AlertCreated.class::isInstance)
                .map(AlertCreated.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static List<RecipientGroup> groups(HasNotificationRequests event) {
        assertThat(event.notificationRequests()).hasSize(1);
        return event.notificationRequests().get(0).recipientGroups();
    }

    private static NotificationLevel level(HasNotificationRequests event) {
        return event.notificationRequests().get(0).level().orElseThrow();
    }

    private static com.eldercare.backend.common.notification.Substitution anySubstitution() {
        return new com.eldercare.backend.common.notification.Substitution.Default();
    }
}
