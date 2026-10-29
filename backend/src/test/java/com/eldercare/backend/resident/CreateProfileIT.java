package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentCreated;
import com.eldercare.backend.resident.internal.profile.domain.Contact;
import com.eldercare.backend.resident.internal.profile.domain.IdDocumentType;
import com.eldercare.backend.resident.internal.profile.domain.PersonalInfo;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService.CreateResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T014 (US1 #1; FR-001, FR-002, FR-003; DBR-23): tạo hồ sơ cấp mã duy nhất, trạng thái Đang tiếp nhận, bản ghi
 * lịch sử đầu tiên kèm người tạo và thời điểm, đúng một bản ghi nhật ký, phát {@code ResidentCreated}.
 */
class CreateProfileIT extends ResidentProfileTestBase {

    @Autowired
    ApplicationEvents events;

    @Test
    void US1_1_create_issuesUniqueCodeAndAdmissionStatus() {
        String name = uniqueName();

        CreateResult first = createAsOffice(noDocument(name));
        CreateResult second = createAsOffice(noDocument(uniqueName()));

        assertThat(first.profile().profileCode()).matches("NCT-\\d{4}-\\d{6}");
        assertThat(first.profile().profileCode()).isNotEqualTo(second.profile().profileCode());
        assertThat(first.profile().id()).isPositive();
        assertThat(first.profile().status()).isEqualTo(ResidentStatus.ADMISSION_IN_PROGRESS);
        assertThat(first.profile().personalInfo().fullName()).isEqualTo(name);
        assertThat(first.profile().currentCareLevel()).isNull();
        assertThat(first.profile().previousProfileId()).isNull();
        assertThat(first.warnings()).isEmpty();
    }

    @Test
    void US1_1_create_keepsEveryFieldOfThePersonalInfo() {
        user.set(OFFICE);
        PersonalInfo info = new PersonalInfo(
                uniqueName(),
                LocalDate.of(1940, 12, 1),
                Gender.FEMALE,
                IdDocumentType.OTHER,
                "HC-" + uniqueCitizenId(),
                null,
                "12 Lê Lợi, Quận 1",
                "34 Trần Hưng Đạo, Quận 5",
                new Contact("0901234567", "co@example.com"),
                "Đi lại cần gậy");

        PersonalInfo saved = profiles.create(info, false).profile().personalInfo();

        assertThat(saved).isEqualTo(info);
    }

    @Test
    void US1_1_create_writesFirstStatusHistoryWithCreatorAndTime() {
        CreateResult created = createAsOffice(noDocument(uniqueName()));

        List<Map<String, Object>> history = jdbc.queryForList(
                "SELECT from_status, to_status, command, performed_by, by_system, reason, occurred_at"
                        + " FROM resident_status_history WHERE resident_id = ?",
                created.profile().id());

        assertThat(history).hasSize(1);
        Map<String, Object> row = history.getFirst();
        assertThat(row.get("from_status")).isNull();
        assertThat(row.get("to_status")).isEqualTo("ADMISSION_IN_PROGRESS");
        assertThat(row.get("command")).isEqualTo("CREATE_PROFILE");
        assertThat(((Number) row.get("performed_by")).longValue()).isEqualTo(OFFICE.accountId());
        assertThat(row.get("by_system")).isIn(false, 0);
        assertThat(row.get("reason")).isNull();
        assertThat(row.get("occurred_at")).isNotNull();
    }

    @Test
    void DBR_23_create_writesExactlyOneAuditEntryAndPublishesEvent() {
        CreateResult created = createAsOffice(noDocument(uniqueName()));
        long id = created.profile().id();

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M01.RESIDENT' AND entity_id = ?"
                                + " AND action = 'RESIDENT_CREATED' AND resident_id = ? AND actor_id = ?",
                        Long.class,
                        String.valueOf(id),
                        id,
                        OFFICE.accountId()))
                .isEqualTo(1);
        assertThat(events.stream(ResidentCreated.class).map(ResidentCreated::residentId))
                .contains(id);
    }

    @Test
    void FR_001_create_requiresNameBirthDateAndGender() {
        user.set(OFFICE);

        assertThatThrownBy(() -> profiles.create(personal(" ", BORN, Gender.MALE, null, null), false))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> profiles.create(personal(uniqueName(), null, Gender.MALE, null, null), false))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> profiles.create(personal(uniqueName(), BORN, null, null, null), false))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void FR_001_create_rejectsDocumentNumberWithoutTypeAndFutureBirthDate() {
        user.set(OFFICE);

        assertThatThrownBy(() -> profiles.create(personal(uniqueName(), BORN, Gender.MALE, null, "0123"), false))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> profiles.create(
                        personal(uniqueName(), LocalDate.now().plusDays(1), Gender.MALE, null, null), false))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void FR_001_create_isOnlyForRolesWithCreatePermission() {
        user.set(DOCTOR);

        assertThatThrownBy(() -> profiles.create(noDocument(uniqueName()), false))
                .isInstanceOf(PermissionDeniedException.class);
    }
}
