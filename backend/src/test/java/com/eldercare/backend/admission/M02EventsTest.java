package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.events.AddendumSignatureOverdue;
import com.eldercare.backend.admission.api.events.AddendumSignatureReminder;
import com.eldercare.backend.admission.api.events.ContractOverdueEscalated;
import com.eldercare.backend.admission.api.events.DeathRecorded;
import com.eldercare.backend.admission.api.events.DepositNotSatisfiedAfterAdmission;
import com.eldercare.backend.admission.api.events.ResidencyChangeOutcomeNotice;
import com.eldercare.backend.admission.api.events.ResidencyChangeReturnedToDraft;
import com.eldercare.backend.admission.api.events.ResidencyChangeSubmittedNotice;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
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

    /** FR-043d (Q-420): nhắc ký phụ lục cho Hành chính, mức Nhẹ; quá hạn ký báo Quản lý viện, mức Trung bình. */
    @Test
    void FR_043d_addendumSignatureNotices() {
        NotificationRequest reminder = single(new AddendumSignatureReminder(5L, 9L, 11L, 2, AT));
        assertThat(reminder.level()).contains(NotificationLevel.LOW);
        assertRecipient(reminder, Role.OFFICE_STAFF);
        assertThat(reminder.sourceRecord().type()).isEqualTo("M02.RESIDENCY_CHANGE");

        NotificationRequest overdue = single(new AddendumSignatureOverdue(5L, 9L, 11L, 2, AT));
        assertThat(overdue.level()).contains(NotificationLevel.MEDIUM);
        assertRecipient(overdue, Role.NURSING_HOME_MANAGER);
    }

    /** FR-043e, FR-043g (Q-419, Q-422): trả về nháp báo Hành chính và người lập; kết quả, gửi duyệt báo người đại diện. */
    @Test
    void FR_043e_g_residencyChangeNotices() {
        NotificationRequest returned = single(new ResidencyChangeReturnedToDraft(9L, 11L, 40L, "Sửa ngày", AT));
        assertThat(returned.level()).contains(NotificationLevel.LOW);
        assertThat(returned.recipientGroups())
                .contains(
                        new RecipientGroup.FacilityRole(Role.OFFICE_STAFF), new RecipientGroup.Staff(40L, "REQUESTER"));

        NotificationRequest outcome = single(new ResidencyChangeOutcomeNotice(
                9L, 11L, ResidencyChangeOutcomeNotice.Outcome.REJECTED, Set.of("BED"), AT));
        assertThat(outcome.level()).contains(NotificationLevel.LOW);
        assertThat(outcome.recipientGroups()).containsExactly(new RecipientGroup.Representative(11L));

        NotificationRequest submitted = single(
                new ResidencyChangeSubmittedNotice(9L, 11L, Set.of("CARE_LEVEL"), LocalDate.of(2030, 3, 9), 77L, AT));
        assertThat(submitted.recipientGroups()).containsExactly(new RecipientGroup.Representative(11L));
        assertThat(submitted.causedBy()).contains(77L);
        assertThat(submitted.notifyCauser()).isFalse();
    }

    @Test
    void eventsForOtherModules_doNotSendNotifications() {
        assertThat(HasNotificationRequests.class.isAssignableFrom(DeathRecorded.class))
                .isFalse();
    }
}
