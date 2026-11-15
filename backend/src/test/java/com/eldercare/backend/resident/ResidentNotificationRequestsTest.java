package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.HealthFields;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.events.ResidentStatusNotice;
import com.eldercare.backend.resident.api.events.SelfDeterminationReviewReminder;
import com.eldercare.backend.resident.api.events.TemporaryAbsenceOverdue;
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
        assertThat(r.sourceRecord()).isEqualTo(new SourceRecord("M01.RESIDENT_STATUS_HISTORY", "900"));
        assertThat(r.occurredAt()).isEqualTo(AT);
        assertThat(r.afterDeath()).isFalse();
        assertThat(r.content()).allSatisfy(p -> assertThat(p.type()).isEqualTo(InformationType.GENERAL));
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
}
