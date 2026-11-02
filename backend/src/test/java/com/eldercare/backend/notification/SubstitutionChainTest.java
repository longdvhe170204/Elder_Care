package com.eldercare.backend.notification;

import static com.eldercare.backend.notification.internal.recipient.domain.SubstitutionStep.FLOOR_MANAGER;
import static com.eldercare.backend.notification.internal.recipient.domain.SubstitutionStep.FLOOR_MANAGER_SUBSTITUTE;
import static com.eldercare.backend.notification.internal.recipient.domain.SubstitutionStep.MANAGERS;
import static com.eldercare.backend.notification.internal.recipient.domain.SubstitutionStep.SHIFT_LEAD;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.notification.internal.recipient.domain.SubstitutionChain;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * FR-010, Q-110, Q-344 ({@code ChuoiThay}; research B-04): thứ tự thay mặc định. Điều dưỡng phụ trách → Người phụ trách ca
 * của tầng → Trưởng tầng được giao → Quản lý viện; nhóm (a) khác → Trưởng tầng được giao → Quản lý viện; Trưởng tầng trống
 * thì Người phụ trách ca đang diễn ra của tầng (Q-90) trước Quản lý viện.
 */
class SubstitutionChainTest {

    private static final Substitution DEFAULT = new Substitution.Default();

    @Test
    void assignedNurse_shiftLead_floorManager_thenManagers() {
        assertThat(SubstitutionChain.steps(new RecipientGroup.AssignedNurse(7L, DEFAULT)))
                .containsExactly(SHIFT_LEAD, FLOOR_MANAGER, FLOOR_MANAGER_SUBSTITUTE, MANAGERS);
    }

    @Test
    void otherDutyGroups_floorManager_itsSubstitute_thenManagers() {
        assertThat(SubstitutionChain.steps(new RecipientGroup.ShiftLead(2L, DEFAULT)))
                .containsExactly(FLOOR_MANAGER, FLOOR_MANAGER_SUBSTITUTE, MANAGERS);
        assertThat(SubstitutionChain.steps(new RecipientGroup.OnShiftStaff(2L, Set.of(), Set.of(), DEFAULT)))
                .containsExactly(FLOOR_MANAGER, FLOOR_MANAGER_SUBSTITUTE, MANAGERS);
        assertThat(SubstitutionChain.steps(
                        new RecipientGroup.AssignedStaff(Optional.of(7L), Optional.empty(), Optional.empty(), DEFAULT)))
                .containsExactly(FLOOR_MANAGER, FLOOR_MANAGER_SUBSTITUTE, MANAGERS);
    }

    @Test
    void floorManagerGroup_shiftLeadOfFloor_thenManagers() {
        assertThat(SubstitutionChain.steps(new RecipientGroup.FloorManager(2L, DEFAULT)))
                .containsExactly(FLOOR_MANAGER_SUBSTITUTE, MANAGERS);
    }
}
