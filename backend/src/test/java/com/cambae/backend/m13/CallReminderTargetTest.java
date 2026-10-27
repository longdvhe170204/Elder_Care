package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m13.internal.urgent.domain.CallReminderTarget;
import com.cambae.backend.m13.internal.urgent.domain.CallReminderTarget.Candidate;
import com.cambae.backend.m13.internal.urgent.domain.CallReminderTarget.Target;
import com.cambae.backend.m13.internal.urgent.domain.CallReminderTarget.Tier;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * T016 (tên logic {@code NguoiNhacGoiTest}): người được nhắc gọi theo FR-029: Người phụ trách ca của tầng → Trưởng tầng
 * được giao → Quản lý viện; bỏ người vắng ca, tài khoản không Hoạt động và chính người cần gọi.
 */
class CallReminderTargetTest {

    private static final Candidate LEAD = new Candidate(10L, true);
    private static final Candidate FLOOR_MANAGER = new Candidate(20L, true);
    private static final List<Candidate> MANAGERS = List.of(new Candidate(30L, true), new Candidate(31L, true));

    @Test
    void FR_029_shiftLeadOfFloorFirst() {
        assertThat(CallReminderTarget.of(Optional.of(LEAD), Optional.of(FLOOR_MANAGER), MANAGERS, Optional.empty()))
                .contains(new Target(Tier.SHIFT_LEAD, List.of(10L)));
    }

    @Test
    void FR_029_absentOrInactiveShiftLead_skippedToFloorManager() {
        assertThat(CallReminderTarget.of(
                        Optional.of(new Candidate(10L, false)), Optional.of(FLOOR_MANAGER), MANAGERS, Optional.empty()))
                .contains(new Target(Tier.FLOOR_MANAGER, List.of(20L)));
        assertThat(CallReminderTarget.of(Optional.empty(), Optional.of(FLOOR_MANAGER), MANAGERS, Optional.empty()))
                .contains(new Target(Tier.FLOOR_MANAGER, List.of(20L)));
    }

    @Test
    void FR_029_personToCallIsNeverTheReminded() {
        assertThat(CallReminderTarget.of(Optional.of(LEAD), Optional.of(FLOOR_MANAGER), MANAGERS, Optional.of(10L)))
                .contains(new Target(Tier.FLOOR_MANAGER, List.of(20L)));
        assertThat(CallReminderTarget.of(Optional.empty(), Optional.empty(), MANAGERS, Optional.of(30L)))
                .contains(new Target(Tier.MANAGERS, List.of(31L)));
    }

    @Test
    void FR_029_noOneOnFloor_orNoFloor_goesToActiveManagers() {
        List<Candidate> managers = List.of(new Candidate(30L, false), new Candidate(31L, true));
        assertThat(CallReminderTarget.of(
                        Optional.empty(), Optional.of(new Candidate(20L, false)), managers, Optional.empty()))
                .contains(new Target(Tier.MANAGERS, List.of(31L)));
        // Sự kiện không gắn tầng/khu vực: không có bậc (1), (2)
        assertThat(CallReminderTarget.of(Optional.empty(), Optional.empty(), managers, Optional.empty()))
                .contains(new Target(Tier.MANAGERS, List.of(31L)));
    }

    @Test
    void FR_030a_overdue_nextTierAfterCurrent_managerTierStaysManager() {
        assertThat(CallReminderTarget.after(
                        Tier.SHIFT_LEAD, Optional.of(LEAD), Optional.of(FLOOR_MANAGER), MANAGERS, Optional.empty()))
                .contains(new Target(Tier.FLOOR_MANAGER, List.of(20L)));
        assertThat(CallReminderTarget.after(
                        Tier.SHIFT_LEAD, Optional.of(LEAD), Optional.empty(), MANAGERS, Optional.empty()))
                .contains(new Target(Tier.MANAGERS, List.of(30L, 31L)));
        assertThat(CallReminderTarget.after(
                        Tier.FLOOR_MANAGER, Optional.of(LEAD), Optional.of(FLOOR_MANAGER), MANAGERS, Optional.empty()))
                .contains(new Target(Tier.MANAGERS, List.of(30L, 31L)));
        assertThat(CallReminderTarget.after(
                        Tier.MANAGERS, Optional.of(LEAD), Optional.of(FLOOR_MANAGER), MANAGERS, Optional.empty()))
                .contains(new Target(Tier.MANAGERS, List.of(30L, 31L)));
    }

    @Test
    void FR_029_noEligiblePersonAtAnyTier_empty() {
        assertThat(CallReminderTarget.of(
                        Optional.of(new Candidate(10L, false)),
                        Optional.empty(),
                        List.of(new Candidate(30L, false)),
                        Optional.empty()))
                .isEmpty();
    }
}
