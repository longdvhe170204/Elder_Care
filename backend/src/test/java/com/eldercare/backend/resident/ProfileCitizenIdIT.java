package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.internal.profile.domain.IdDocumentType;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService.CreateResult;
import org.junit.jupiter.api.Test;

/**
 * 001 T014 (US1 #2, #3, #10; DBR-01, FR-003, FR-004): CCCD duy nhất trong hồ sơ chưa ở trạng thái cuối; trùng hồ sơ Kết
 * thúc lưu trú, Hủy tiếp nhận thì tạo và liên kết hồ sơ gần nhất; trùng hồ sơ Qua đời thì tạo, không liên kết, kèm cảnh
 * báo.
 */
class ProfileCitizenIdIT extends ResidentProfileTestBase {

    @Test
    void US1_2_citizenIdOfOpenProfile_isRejectedWithExistingProfileCode() {
        String cccd = uniqueCitizenId();
        CreateResult existing = createAsOffice(withCitizenId(uniqueName(), cccd));

        assertThatThrownBy(() -> profiles.create(withCitizenId(uniqueName(), cccd), false))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(409);
                    assertThat(e.errorCode()).isEqualTo("CITIZEN_ID_IN_USE");
                    assertThat(e.properties())
                            .containsEntry("profileCode", existing.profile().profileCode());
                });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM resident WHERE id_document_number = ?", Long.class, cccd))
                .isEqualTo(1);
    }

    @Test
    void US1_2_everyNonFinalStatus_blocksTheSameCitizenId() {
        for (String status : new String[] {"IN_RESIDENCE", "TEMPORARILY_ABSENT", "OFF_SITE_ACTIVITY", "HOSPITALIZED"}) {
            String cccd = uniqueCitizenId();
            long id =
                    createAsOffice(withCitizenId(uniqueName(), cccd)).profile().id();
            setStatus(id, status);

            assertThatThrownBy(() -> profiles.create(withCitizenId(uniqueName(), cccd), false))
                    .as(status)
                    .isInstanceOfSatisfying(
                            BusinessException.class,
                            e -> assertThat(e.errorCode()).isEqualTo("CITIZEN_ID_IN_USE"));
        }
    }

    @Test
    void US1_3_citizenIdOfEndedOrCancelledProfile_createsAndLinksPreviousProfile() {
        for (String status : new String[] {"RESIDENCY_ENDED", "ADMISSION_CANCELLED"}) {
            String cccd = uniqueCitizenId();
            long old =
                    createAsOffice(withCitizenId(uniqueName(), cccd)).profile().id();
            setStatus(old, status);

            CreateResult created = profiles.create(withCitizenId(uniqueName(), cccd), false);

            assertThat(created.profile().previousProfileId()).as(status).isEqualTo(old);
            assertThat(created.warnings()).as(status).isEmpty();
            assertThat(created.profile().id()).isNotEqualTo(old);
            // Hồ sơ cũ giữ nguyên
            assertThat(jdbc.queryForObject("SELECT status FROM resident WHERE id = ?", String.class, old))
                    .isEqualTo(status);
        }
    }

    @Test
    void US1_10_citizenIdOfDeceasedProfile_createsWithoutLinkAndWarns() {
        String cccd = uniqueCitizenId();
        long deceased =
                createAsOffice(withCitizenId(uniqueName(), cccd)).profile().id();
        setStatus(deceased, "DECEASED");

        CreateResult created = profiles.create(withCitizenId(uniqueName(), cccd), false);

        assertThat(created.profile().previousProfileId()).isNull();
        assertThat(created.warnings())
                .singleElement()
                .satisfies(w -> assertThat(w.code()).isEqualTo("CITIZEN_ID_MATCHES_DECEASED_PROFILE"));
    }

    @Test
    void US1_10_withTwoOldProfiles_newProfileLinksTheMostRecentOne() {
        String cccd = uniqueCitizenId();
        long h1 = createAsOffice(withCitizenId(uniqueName(), cccd)).profile().id();
        setStatus(h1, "RESIDENCY_ENDED");
        long h2 = profiles.create(withCitizenId(uniqueName(), cccd), false)
                .profile()
                .id();
        setStatus(h2, "RESIDENCY_ENDED");

        CreateResult h4 = profiles.create(withCitizenId(uniqueName(), cccd), false);

        assertThat(h4.profile().previousProfileId()).isEqualTo(h2);
    }

    @Test
    void DBR_01_otherDocumentTypes_areNotUniqueAndDoNotLink() {
        String number = "HC-" + uniqueCitizenId();
        user.set(OFFICE);

        CreateResult first =
                profiles.create(personal(uniqueName(), BORN, Gender.MALE, IdDocumentType.OTHER, number), false);
        CreateResult second =
                profiles.create(personal(uniqueName(), BORN, Gender.MALE, IdDocumentType.OTHER, number), false);

        assertThat(second.profile().id()).isNotEqualTo(first.profile().id());
        assertThat(second.profile().previousProfileId()).isNull();
    }

    @Test
    void DBR_01_citizenIdIsMatchedAfterTrimmingSpaces() {
        String cccd = uniqueCitizenId();
        createAsOffice(withCitizenId(uniqueName(), cccd));

        assertThatThrownBy(() -> profiles.create(withCitizenId(uniqueName(), " " + cccd + " "), false))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("CITIZEN_ID_IN_USE"));
    }
}
