package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.error.ReasonRequiredException;
import com.cambae.backend.common.error.VersionMismatchException;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m01.api.events.PersonalInfoUpdated;
import com.cambae.backend.m01.internal.profile.domain.IdDocumentType;
import com.cambae.backend.m01.internal.profile.domain.PersonalInfo;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService.ProfileView;
import com.cambae.backend.m15.account.api.dto.Target;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T015 (US1 #5; FR-006, BR-M01-05; DBR-23): lệnh Cập nhật thông tin cá nhân cần lý do và {@code If-Match}; đổi giới
 * tính (và mọi trường) phát {@code PersonalInfoUpdated}; nhật ký chỉ ghi trường đã đổi, trước và sau; hồ sơ trạng thái
 * cuối bị chặn.
 */
class UpdatePersonalInfoIT extends ResidentProfileTestBase {

    private static final Reason REASON = new Reason("Sửa theo giấy tờ gốc");

    @Autowired
    ApplicationEvents events;

    private ProfileView created() {
        return createAsOffice(noDocument(uniqueName())).profile();
    }

    private static Target target(ProfileView profile) {
        return ResidentProfileService.target(profile.id());
    }

    private static String etag(ProfileView profile) {
        return VersionEtag.of(profile.version());
    }

    @Test
    void US1_5_update_appliesNewValuesAndRaisesTheVersion() {
        ProfileView before = created();
        PersonalInfo changed = new PersonalInfo(
                before.personalInfo().fullName() + " Sửa",
                before.personalInfo().dateOfBirth(),
                Gender.MALE,
                null,
                null,
                null,
                "Địa chỉ mới",
                null,
                null,
                null);

        ProfileView after = profiles.updatePersonalInfo(target(before), etag(before), changed, REASON);

        assertThat(after.personalInfo()).isEqualTo(changed);
        assertThat(after.version()).isGreaterThan(before.version());
        assertThat(after.id()).isEqualTo(before.id());
        assertThat(after.profileCode()).isEqualTo(before.profileCode());
        assertThat(after.status()).isEqualTo(before.status());
    }

    @Test
    void FR_006_staleIfMatch_isRejectedWithCurrentValue() {
        ProfileView before = created();
        profiles.updatePersonalInfo(target(before), etag(before), withName(before, "Tên mới 1"), REASON);

        assertThatThrownBy(() -> profiles.updatePersonalInfo(
                        target(before), etag(before), withName(before, "Tên mới 2"), REASON))
                .isInstanceOfSatisfying(VersionMismatchException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(412);
                    assertThat(e.currentValue())
                            .isInstanceOfSatisfying(
                                    ProfileView.class,
                                    current -> assertThat(current.personalInfo().fullName())
                                            .isEqualTo("Tên mới 1"));
                });
        assertThat(jdbc.queryForObject("SELECT full_name FROM resident WHERE id = ?", String.class, before.id()))
                .isEqualTo("Tên mới 1");
    }

    @Test
    void FR_006_missingIfMatch_isRejected() {
        ProfileView before = created();

        assertThatThrownBy(
                        () -> profiles.updatePersonalInfo(target(before), null, withName(before, "Tên khác"), REASON))
                .isInstanceOf(VersionMismatchException.class);
    }

    @Test
    void US1_5_missingReason_isRejectedAndNothingChanges() {
        ProfileView before = created();

        assertThatThrownBy(() ->
                        profiles.updatePersonalInfo(target(before), etag(before), withName(before, "Tên khác"), null))
                .isInstanceOf(ReasonRequiredException.class);
        assertThat(jdbc.queryForObject("SELECT full_name FROM resident WHERE id = ?", String.class, before.id()))
                .isEqualTo(before.personalInfo().fullName());
    }

    @Test
    void FR_006_changeOfGender_publishesPersonalInfoUpdated() {
        ProfileView before = created();
        PersonalInfo female = personal(before.personalInfo().fullName(), BORN, Gender.FEMALE, null, null);

        profiles.updatePersonalInfo(target(before), etag(before), female, REASON);

        assertThat(events.stream(PersonalInfoUpdated.class)
                        .filter(e -> e.residentId() == before.id())
                        .toList())
                .singleElement()
                .satisfies(e -> assertThat(e.changedFields()).containsExactly("gender"));
    }

    @Test
    void DBR_23_update_auditsOnlyChangedFieldsWithReasonAndOneEntry() {
        ProfileView before = created();
        PersonalInfo changed = new PersonalInfo(
                before.personalInfo().fullName(),
                before.personalInfo().dateOfBirth(),
                Gender.MALE,
                null,
                null,
                null,
                "Số 9 Nguyễn Huệ",
                null,
                null,
                null);

        profiles.updatePersonalInfo(target(before), etag(before), changed, REASON);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT reason, resident_id, data_before, data_after FROM audit_log"
                        + " WHERE entity_type = 'M01.RESIDENT' AND entity_id = ? AND action = 'RESIDENT_UPDATED'",
                String.valueOf(before.id()));
        assertThat(row.get("reason")).isEqualTo(REASON.value());
        assertThat(((Number) row.get("resident_id")).longValue()).isEqualTo(before.id());
        assertThat(row.get("data_after").toString()).contains("permanentAddress", "Số 9 Nguyễn Huệ");
        assertThat(row.get("data_after").toString()).doesNotContain("fullName", "gender");
        assertThat(row.get("data_before").toString()).doesNotContain("fullName");
    }

    @Test
    void FR_006_updateWithoutAnyChange_writesNoAuditAndKeepsTheVersion() {
        ProfileView before = created();

        ProfileView same = profiles.updatePersonalInfo(target(before), etag(before), before.personalInfo(), REASON);

        assertThat(same.version()).isEqualTo(before.version());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M01.RESIDENT' AND entity_id = ?"
                                + " AND action = 'RESIDENT_UPDATED'",
                        Long.class,
                        String.valueOf(before.id())))
                .isZero();
        assertThat(events.stream(PersonalInfoUpdated.class).filter(e -> e.residentId() == before.id()))
                .isEmpty();
    }

    @Test
    void DBR_01_update_toCitizenIdOfAnotherOpenProfile_isRejected() {
        String taken = uniqueCitizenId();
        createAsOffice(withCitizenId(uniqueName(), taken));
        ProfileView before = created();
        PersonalInfo clash = withCitizenId(before.personalInfo().fullName(), taken);

        assertThatThrownBy(() -> profiles.updatePersonalInfo(target(before), etag(before), clash, REASON))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("CITIZEN_ID_IN_USE"));
    }

    @Test
    void DBR_01_update_keepingItsOwnCitizenId_isAllowed() {
        String cccd = uniqueCitizenId();
        ProfileView before = createAsOffice(withCitizenId(uniqueName(), cccd)).profile();
        PersonalInfo same =
                personal(before.personalInfo().fullName() + " B", BORN, Gender.MALE, IdDocumentType.CITIZEN_ID, cccd);

        ProfileView after = profiles.updatePersonalInfo(target(before), etag(before), same, REASON);

        assertThat(after.personalInfo().idDocumentNumber()).isEqualTo(cccd);
    }

    @Test
    void BR_M01_05_update_onFinalStatusProfile_isClosed() {
        for (String status : new String[] {"RESIDENCY_ENDED", "DECEASED", "ADMISSION_CANCELLED"}) {
            ProfileView before = created();
            setStatus(before.id(), status);

            assertThatThrownBy(() -> profiles.updatePersonalInfo(
                            target(before), etag(before), withName(before, "Sau khi đóng"), REASON))
                    .as(status)
                    .isInstanceOfSatisfying(
                            BusinessException.class,
                            e -> assertThat(e.errorCode()).isEqualTo("PROFILE_CLOSED"));
        }
    }

    @Test
    void FR_006_update_requiresValidFields() {
        ProfileView before = created();

        assertThatThrownBy(
                        () -> profiles.updatePersonalInfo(target(before), etag(before), withName(before, "  "), REASON))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> profiles.updatePersonalInfo(
                        target(before),
                        etag(before),
                        personal("Tên", LocalDate.now().plusDays(1), Gender.MALE, null, null),
                        REASON))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void FR_006_update_isOnlyForRolesWithUpdatePermission() {
        ProfileView before = created();
        user.set(DOCTOR);

        assertThatThrownBy(() -> profiles.updatePersonalInfo(
                        target(before), etag(before), withName(before, "Bác sĩ sửa"), REASON))
                .isInstanceOf(PermissionDeniedException.class);
    }

    private PersonalInfo withName(ProfileView profile, String fullName) {
        PersonalInfo p = profile.personalInfo();
        return new PersonalInfo(
                fullName,
                p.dateOfBirth(),
                p.gender(),
                p.idDocumentType(),
                p.idDocumentNumber(),
                p.photoFileId(),
                p.permanentAddress(),
                p.contactAddress(),
                p.contact(),
                p.specialNotes());
    }
}
