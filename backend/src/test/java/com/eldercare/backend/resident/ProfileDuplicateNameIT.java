package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.internal.profile.domain.PersonalInfo;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService.CreateResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 001 T014 (US1 #4; FR-005; research H-04): không có CCCD mà trùng họ tên và ngày sinh thì hỏi xác nhận; so khớp họ tên
 * không phân biệt hoa thường và khoảng trắng thừa, có phân biệt dấu; ngày sinh chính xác; nhật ký ghi lần xác nhận.
 */
class ProfileDuplicateNameIT extends ResidentProfileTestBase {

    /** Hậu tố riêng cho mỗi test để các test không trùng nhau. */
    private static String stem() {
        return uniqueCitizenId();
    }

    @Test
    void US1_4_sameNameAndBirthDateWithoutCitizenId_asksForConfirmation() {
        String name = "Trần Thị " + stem();
        CreateResult existing = createAsOffice(noDocument(name));

        assertThatThrownBy(() -> profiles.create(noDocument(name), false))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(409);
                    assertThat(e.errorCode()).isEqualTo("POSSIBLE_DUPLICATE_PROFILE");
                    assertThat((List<?>) e.properties().get("candidates"))
                            .singleElement()
                            .isInstanceOfSatisfying(Map.class, c -> {
                                assertThat(c.get("id"))
                                        .isEqualTo(existing.profile().id());
                                assertThat(c.get("profileCode"))
                                        .isEqualTo(existing.profile().profileCode());
                            });
                });
    }

    @Test
    void US1_4_resendWithConfirmation_createsTheProfileAndAuditsTheConfirmation() {
        String name = "Lê Thị " + stem();
        createAsOffice(noDocument(name));

        CreateResult created = profiles.create(noDocument(name), true);

        assertThat(created.profile().previousProfileId()).isNull();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M01.RESIDENT' AND entity_id = ?"
                                + " AND action = 'RESIDENT_CREATED'"
                                + " AND JSON_EXTRACT(details, '$.confirmedNotDuplicate') = TRUE",
                        Long.class,
                        String.valueOf(created.profile().id())))
                .isEqualTo(1);
    }

    @Test
    void FR_005_matchIgnoresCaseAndExtraSpaces_butNotDiacritics() {
        String stem = stem();
        createAsOffice(noDocument("Phạm Văn " + stem));

        assertThatThrownBy(() -> profiles.create(noDocument("  PHẠM  văn   " + stem.toUpperCase() + " "), false))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("POSSIBLE_DUPLICATE_PROFILE"));
        // Khác dấu là người khác: "Phạm" ≠ "Pham"
        assertThat(profiles.create(noDocument("Pham Van " + stem), false)
                        .profile()
                        .id())
                .isPositive();
    }

    @Test
    void FR_005_differentBirthDate_isNotADuplicate() {
        String name = "Võ Thị " + stem();
        createAsOffice(noDocument(name));

        CreateResult other = profiles.create(personal(name, BORN.plusDays(1), Gender.MALE, null, null), false);

        assertThat(other.profile().id()).isPositive();
    }

    @Test
    void FR_005_profileWithCitizenId_isNotAskedAboutSameName() {
        String name = "Đặng Văn " + stem();
        createAsOffice(noDocument(name));

        CreateResult created = profiles.create(withCitizenId(name, uniqueCitizenId()), false);

        assertThat(created.profile().id()).isPositive();
    }

    @Test
    void FR_005_matchesProfilesInAnyStatus_butNeverLinksThem() {
        String name = "Hoàng Thị " + stem();
        long old = createAsOffice(noDocument(name)).profile().id();
        setStatus(old, "RESIDENCY_ENDED");

        assertThatThrownBy(() -> profiles.create(noDocument(name), false))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("POSSIBLE_DUPLICATE_PROFILE"));
        // Q-319: chỉ liên kết khi trùng CCCD
        assertThat(profiles.create(noDocument(name), true).profile().previousProfileId())
                .isNull();
    }

    @Test
    void FR_005_candidatesAreVisibleToTheCreatorOnlyAsIdentityFields() {
        String name = "Bùi Văn " + stem();
        createAsOffice(new PersonalInfo(name, BORN, Gender.MALE, null, null, null, "Số 1", null, null, null));

        assertThatThrownBy(() -> profiles.create(noDocument(name), false))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(((List<?>) e.properties().get("candidates")).getFirst())
                                .isInstanceOfSatisfying(
                                        Map.class,
                                        c -> assertThat(c.keySet())
                                                .containsExactlyInAnyOrder(
                                                        (Object) "id",
                                                        "profileCode",
                                                        "fullName",
                                                        "dateOfBirth",
                                                        "status")));
    }
}
