package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService.LinkedProfile;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 001 T015 (US1 #3, #10; FR-004): chuỗi hồ sơ trước của cùng người, gần nhất trước. Mỗi phần tử chỉ đầy đủ khi người xem
 * có quyền trên chính hồ sơ đó; còn lại chỉ có mã hồ sơ và {@code viewable = false}.
 */
class ProfileLinkIT extends ResidentProfileTestBase {

    /** H1 (2025) → H2 (2026) → H4, cùng CCCD; trả về id theo thứ tự H1, H2, H4. */
    private long[] chainOfThree() {
        String cccd = uniqueCitizenId();
        long h1 = createAsOffice(withCitizenId(uniqueName(), cccd)).profile().id();
        setStatus(h1, "RESIDENCY_ENDED");
        long h2 = profiles.create(withCitizenId(uniqueName(), cccd), false)
                .profile()
                .id();
        setStatus(h2, "ADMISSION_CANCELLED");
        long h4 = profiles.create(withCitizenId(uniqueName(), cccd), false)
                .profile()
                .id();
        return new long[] {h1, h2, h4};
    }

    @Test
    void US1_10_linkedProfiles_listsTheWholeChainNearestFirst() {
        long[] chain = chainOfThree();
        user.set(OFFICE);

        List<LinkedProfile> linked = profiles.linkedProfiles(ResidentProfileService.target(chain[2]));

        assertThat(linked).extracting(LinkedProfile::id).containsExactly(chain[1], chain[0]);
        assertThat(linked).extracting(LinkedProfile::viewable).containsOnly(true);
        assertThat(linked)
                .extracting(LinkedProfile::status)
                .containsExactly(ResidentStatus.ADMISSION_CANCELLED, ResidentStatus.RESIDENCY_ENDED);
        assertThat(linked.getFirst().profileCode()).matches("NCT-\\d{4}-\\d{6}");
    }

    @Test
    void US1_3_profileWithoutPreviousProfile_hasEmptyChain() {
        long id = createAsOffice(noDocument(uniqueName())).profile().id();

        assertThat(profiles.linkedProfiles(ResidentProfileService.target(id))).isEmpty();
    }

    @Test
    void US1_3_linkedItemWithoutPermissionOnThatProfile_showsOnlyTheCode() {
        long[] chain = chainOfThree();
        // Nhân viên chăm sóc chỉ được phân công H4, nên không có quyền trên H2 và H1
        permissions.assign(chain[2]);
        user.set(CAREGIVER);

        List<LinkedProfile> linked = profiles.linkedProfiles(ResidentProfileService.target(chain[2]));

        assertThat(linked).extracting(LinkedProfile::id).containsExactly(chain[1], chain[0]);
        assertThat(linked).extracting(LinkedProfile::viewable).containsOnly(false);
        assertThat(linked).extracting(LinkedProfile::status).containsOnlyNulls();
        assertThat(linked).extracting(LinkedProfile::profileCode).doesNotContainNull();
    }

    @Test
    void BR_M15_01_linkedProfiles_ofAProfileOutsideScope_isNotFound() {
        long[] chain = chainOfThree();
        user.set(CAREGIVER);

        assertThatThrownBy(() -> profiles.linkedProfiles(ResidentProfileService.target(chain[2])))
                .isInstanceOf(NotFoundException.class);
    }
}
