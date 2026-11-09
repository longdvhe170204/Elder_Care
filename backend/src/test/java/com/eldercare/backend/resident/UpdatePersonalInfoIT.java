package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.internal.profile.domain.PersonalInfo;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService.ProfileView;
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
