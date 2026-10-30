package com.eldercare.backend.administration.correction;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.dto.CreationMethod;
import com.eldercare.backend.administration.correction.internal.domain.CorrectionMethodSelector;
import com.eldercare.backend.administration.correction.internal.domain.CorrectionMethodSelector.Facts;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.FakePermissions;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** FR-026, CHK033, CHK051 (000 T049): bảng chọn cách tạo bản đính chính A/B/B'/C/403 (data-model mục 4). */
class CorrectionMethodSelectorTest {

    private static final Instant NOW = Instant.parse("2026-10-19T03:00:00Z");
    private static final long FLOOR = 2;
    private static final CurrentUser RECORDER = FakePermissions.user(10, Role.NURSING_STAFF);
    private static final CurrentUser SAME_ROLE = FakePermissions.user(11, Role.NURSING_STAFF);
    private static final CurrentUser SHIFT_LEAD = FakePermissions.user(12, Role.NURSING_STAFF);
    private static final CurrentUser FLOOR_MANAGER = FakePermissions.user(13, Role.FLOOR_MANAGER);
    private static final CurrentUser OFFICE_A = FakePermissions.user(20, Role.OFFICE_STAFF);
    private static final CurrentUser OFFICE_B = FakePermissions.user(21, Role.OFFICE_STAFF);

    private static FakePermissions staff() {
        return new FakePermissions()
                .account(RECORDER)
                .account(SAME_ROLE)
                .account(SHIFT_LEAD)
                .account(FLOOR_MANAGER)
                .account(OFFICE_A)
                .account(OFFICE_B);
    }

    private static Optional<CreationMethod> select(FakePermissions fake, CurrentUser user, Facts facts) {
        return new CorrectionMethodSelector(fake, fake).select(user, facts);
    }

    private static Facts floored(long recorder, boolean userCanRecord) {
        return new Facts(recorder, Optional.of(FLOOR), userCanRecord, NOW);
    }

    private static Facts office(long recorder, boolean userCanRecord) {
        return new Facts(recorder, Optional.empty(), userCanRecord, NOW);
    }

    @Test
    void FR_026_a_originalRecorderWithPermission() {
        assertThat(select(staff(), RECORDER, floored(10, true))).contains(CreationMethod.A);
        assertThat(select(staff(), OFFICE_A, office(20, true))).contains(CreationMethod.A);
    }

    @Test
    void FR_026_b_shiftLeadOrFloorManagerOfThatFloor() {
        FakePermissions fake = staff().shiftLead(FLOOR, 12).floorManager(FLOOR, 13);

        assertThat(select(fake, SHIFT_LEAD, floored(10, true))).contains(CreationMethod.B);
        assertThat(select(fake, FLOOR_MANAGER, floored(10, false))).contains(CreationMethod.B);
    }

    @Test
    void CHK033_noShiftLead_assignedFloorManagerStillB() {
        FakePermissions fake = staff().floorManager(FLOOR, 13);

        assertThat(select(fake, FLOOR_MANAGER, floored(10, false))).contains(CreationMethod.B);
    }

    @Test
    void FR_026_sameRoleColleague_onFlooredRecordWithLead_isRefused() {
        FakePermissions fake = staff().shiftLead(FLOOR, 12).floorManager(FLOOR, 13);

        assertThat(select(fake, SAME_ROLE, floored(10, true))).isEmpty();
    }

    @Test
    void CHK033_flooredRecord_nobodyForB_sameRoleColleagueGoesC() {
        assertThat(select(staff(), SAME_ROLE, floored(10, true))).contains(CreationMethod.C);
    }

    @Test
    void FR_026_c_unflooredRecord_sameRoleColleagueGoesC() {
        assertThat(select(staff(), OFFICE_B, office(20, true))).contains(CreationMethod.C);
    }

    @Test
    void CHK051_floorManager_onUnflooredRecord_isRefused() {
        FakePermissions fake = staff().shiftLead(FLOOR, 12).floorManager(FLOOR, 13);

        assertThat(select(fake, FLOOR_MANAGER, office(20, false))).isEmpty();
        assertThat(select(fake, SHIFT_LEAD, office(20, false))).isEmpty();
    }

    @Test
    void FR_026_colleagueWithoutRecordingPermission_isRefused() {
        assertThat(select(staff(), OFFICE_B, office(20, false))).isEmpty();
    }
}
