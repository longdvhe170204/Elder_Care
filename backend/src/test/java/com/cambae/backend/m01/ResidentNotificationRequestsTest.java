package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.HealthFields;
import com.cambae.backend.common.notification.InformationSubject;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.Q354Case;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.SourceRecord;
import com.cambae.backend.common.notification.Substitution;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.api.events.CareLevelChangeRequestNeeded;
import com.cambae.backend.m01.api.events.CareLevelChangeRequestReminder;
import com.cambae.backend.m01.api.events.MissingConsentReminder;
import com.cambae.backend.m01.api.events.MissingEndOfLifeWishReminder;
import com.cambae.backend.m01.api.events.ReassessmentDue;
import com.cambae.backend.m01.api.events.ReassessmentOverdue;
import com.cambae.backend.m01.api.events.ResidentStatusNotice;
import com.cambae.backend.m01.api.events.SelfDeterminationReviewReminder;
import com.cambae.backend.m01.api.events.TemporaryAbsenceOverdue;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 001 T009 (FR-054, Q-97, Q-354; research H-14; 009 FR-001, FR-001a, FR-043a): các sự kiện M01 gửi thông báo qua khuôn
 * {@code common.notification}: mức, nhóm người nhận, loại thông tin, khóa sự kiện chống trùng, đối tượng thông tin và mã
 * Q-354 cho người liên hệ chính. Nội dung rút gọn (kênh ngoài ứng dụng) không chứa họ tên hay trường sức khỏe.
 */
class ResidentNotificationRequestsTest {

    private static final Instant AT = Instant.parse("2026-10-24T03:00:00Z");
    private static final long RESIDENT = 41L;

    private static final RecipientGroup.OnDutyDoctor DOCTOR =
            new RecipientGroup.OnDutyDoctor(new Substitution.Default());
    private static final RecipientGroup OFFICE = new RecipientGroup.FacilityRole(Role.OFFICE_STAFF);
    private static final RecipientGroup MANAGER = new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER);
    private static final RecipientGroup NURSE = new RecipientGroup.AssignedNurse(RESIDENT, new Substitution.Default());

    private static NotificationRequest only(HasNotificationRequests event) {
        List<NotificationRequest> requests = event.notificationRequests();
        assertThat(requests).hasSize(1);
        NotificationRequest r = requests.getFirst();
        assertThat(r.sourceModule()).isEqualTo("M01");
        assertThat(r.residentId()).contains(RESIDENT);
        assertThat(r.occurredAt()).isNotNull();
        assertThat(r.eventKey()).isNotBlank();
        assertThat(r.sourceRecord().type()).startsWith("M01.");
        assertThat(r.content()).isNotEmpty();
        assertThat(r.title()).isPresent();
        // Kênh ngoài ứng dụng: nội dung rút gọn có và không dùng khóa thuộc trường sức khỏe (009 FR-019)
        assertThat(r.shortContent()).isPresent();
        assertThat(r.displayParams().keySet()).noneMatch(HealthFields.KEYS::contains);
        return r;
    }

    private static ResidentStatusNotice status(StatusCommand command) {
        return new ResidentStatusNotice(RESIDENT, command, 900L, AT);
    }

    @Test
    void Q_97_transferOut_isUrgentForThePrimaryContact_withQ354TransferCase() {
        NotificationRequest r = only(status(StatusCommand.TRANSFER_OUT));

        assertThat(r.level()).contains(NotificationLevel.URGENT);
        assertThat(r.recipientGroups()).containsExactly(new RecipientGroup.PrimaryContact(RESIDENT));
        assertThat(r.informationSubject()).contains(InformationSubject.ABOUT_RESIDENT);
        assertThat(r.q354Case()).contains(Q354Case.TRANSFER);
        assertThat(r.sourceRecord()).isEqualTo(new SourceRecord("M01.STATUS_HISTORY", "900"));
        assertThat(r.occurredAt()).isEqualTo(AT);
        assertThat(r.afterDeath()).isFalse();
        assertThat(r.content()).allSatisfy(p -> assertThat(p.type()).isEqualTo(InformationType.GENERAL));
    }

    @Test
    void Q_97_recordDeath_isUrgentForThePrimaryContact_withQ354DeathCase_atTheSameInstantAsTheDeath() {
        NotificationRequest r = only(status(StatusCommand.RECORD_DEATH));

        assertThat(r.level()).contains(NotificationLevel.URGENT);
        assertThat(r.recipientGroups()).containsExactly(new RecipientGroup.PrimaryContact(RESIDENT));
        assertThat(r.q354Case()).contains(Q354Case.DEATH);
        // 009 FR-015: tin báo qua đời phát sinh cùng thời điểm qua đời nên vẫn tới người thân, không cần dấu việc sau
        // qua đời
        assertThat(r.occurredAt()).isEqualTo(AT);
        assertThat(r.afterDeath()).isFalse();
        assertThat(r.eventKey())
                .isNotEqualTo(only(status(StatusCommand.TRANSFER_OUT)).eventKey());
    }

    @Test
    void FR_046_onlyTransferAndDeathCanBeANotice() {
        for (StatusCommand command : StatusCommand.values()) {
            if (command == StatusCommand.TRANSFER_OUT || command == StatusCommand.RECORD_DEATH) {
                continue;
            }
            assertThatThrownBy(() -> status(command)).as(command.name()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void FR_039_reassessmentDue_isLowForTheDoctorOnDuty_healthTextOnlyInTheFullContent() {
        NotificationRequest r = only(new ReassessmentDue(RESIDENT, 7L, "PERIODIC", AT));

        assertThat(r.level()).contains(NotificationLevel.LOW);
        assertThat(r.recipientGroups()).containsExactly(DOCTOR);
        assertThat(r.content()).extracting(p -> p.type()).containsOnly(InformationType.HEALTH);
        assertThat(r.informationSubject()).isEmpty();
        assertThat(r.q354Case()).isEmpty();
        assertThat(r.sourceRecord()).isEqualTo(new SourceRecord("M01.REASSESSMENT_NOTICE", "7"));
    }

    @Test
    void FR_041_reassessmentOverdue_isMediumForTheDoctorOnDuty_keyedByTheDueInstant() {
        Instant due = Instant.parse("2026-10-22T03:00:00Z");
        NotificationRequest r = only(new ReassessmentOverdue(RESIDENT, due));

        assertThat(r.level()).contains(NotificationLevel.MEDIUM);
        assertThat(r.recipientGroups()).containsExactly(DOCTOR);
        assertThat(r.eventKey()).contains(String.valueOf(due.toEpochMilli()));
        assertThat(only(new ReassessmentOverdue(RESIDENT, due.plusSeconds(1))).eventKey())
                .isNotEqualTo(r.eventKey());
    }

    @Test
    void FR_036_careLevelChangeRequest_isMediumTheFirstTimeAndLowForReminders_forOfficeStaff() {
        NotificationRequest first = only(new CareLevelChangeRequestNeeded(RESIDENT, 5L, CareLevel.SPECIAL, AT));
        NotificationRequest second = only(new CareLevelChangeRequestReminder(RESIDENT, 5L, CareLevel.SPECIAL, 2, AT));
        NotificationRequest third = only(new CareLevelChangeRequestReminder(RESIDENT, 5L, CareLevel.SPECIAL, 3, AT));

        assertThat(first.level()).contains(NotificationLevel.MEDIUM);
        assertThat(first.recipientGroups()).containsExactly(OFFICE);
        assertThat(second.level()).contains(NotificationLevel.LOW);
        assertThat(second.recipientGroups()).containsExactly(OFFICE);
        assertThat(List.of(first.eventKey(), second.eventKey(), third.eventKey()))
                .doesNotHaveDuplicates();
    }

    @Test
    void FR_012_missingConsentReminder_isLowForOfficeStaff_eachReminderHasItsOwnKey() {
        NotificationRequest one = only(new MissingConsentReminder(RESIDENT, 1, AT));
        NotificationRequest two = only(new MissingConsentReminder(RESIDENT, 2, AT));

        assertThat(one.level()).contains(NotificationLevel.LOW);
        assertThat(one.recipientGroups()).containsExactly(OFFICE);
        assertThat(one.eventKey()).isNotEqualTo(two.eventKey());
    }

    @Test
    void FR_023d_missingEndOfLifeWishReminder_isLowForTheDoctorAndTheAssignedNurse() {
        NotificationRequest r = only(new MissingEndOfLifeWishReminder(RESIDENT, 1, AT));

        assertThat(r.level()).contains(NotificationLevel.LOW);
        assertThat(r.recipientGroups()).containsExactlyInAnyOrder(DOCTOR, NURSE);
        assertThat(only(new MissingEndOfLifeWishReminder(RESIDENT, 2, AT)).eventKey())
                .isNotEqualTo(r.eventKey());
    }

    @Test
    void FR_023i_selfDeterminationReview_goesToTheAssessingDoctorAndTheAssignedNurse() {
        NotificationRequest r = only(new SelfDeterminationReviewReminder(RESIDENT, 12L, 77L, AT));

        assertThat(r.level()).contains(NotificationLevel.LOW);
        assertThat(r.recipientGroups())
                .containsExactlyInAnyOrder(new RecipientGroup.Staff(77L, "ASSESSING_DOCTOR"), NURSE);
        assertThat(r.sourceRecord()).isEqualTo(new SourceRecord("M01.END_OF_LIFE_WISH", "12"));
    }

    @Test
    void FR_023i_selfDeterminationReview_withoutAKnownDoctor_fallsBackToTheDoctorOnDuty() {
        NotificationRequest r = only(new SelfDeterminationReviewReminder(RESIDENT, 12L, null, AT));

        assertThat(r.recipientGroups()).containsExactlyInAnyOrder(DOCTOR, NURSE);
    }

    @Test
    void FR_050_temporaryAbsenceOverdue_isMediumForTheFloorManagerAndOfficeStaff() {
        Instant expected = Instant.parse("2026-10-24T01:00:00Z");
        NotificationRequest r = only(new TemporaryAbsenceOverdue(RESIDENT, 9L, expected, 3L, AT));

        assertThat(r.level()).contains(NotificationLevel.MEDIUM);
        assertThat(r.recipientGroups())
                .containsExactlyInAnyOrder(new RecipientGroup.FloorManager(3L, new Substitution.Default()), OFFICE);
        assertThat(r.floorAreaId()).contains(3L);
        assertThat(r.sourceRecord()).isEqualTo(new SourceRecord("M01.ABSENCE", "9"));
        assertThat(r.eventKey()).contains("9").contains(String.valueOf(expected.toEpochMilli()));
    }

    @Test
    void FR_050_temporaryAbsenceOverdue_withoutAFloor_goesToOfficeStaffAndTheManager() {
        NotificationRequest r =
                only(new TemporaryAbsenceOverdue(RESIDENT, 9L, Instant.parse("2026-10-24T01:00:00Z"), null, AT));

        assertThat(r.floorAreaId()).isEmpty();
        assertThat(r.recipientGroups()).containsExactlyInAnyOrder(OFFICE, MANAGER);
    }
}
