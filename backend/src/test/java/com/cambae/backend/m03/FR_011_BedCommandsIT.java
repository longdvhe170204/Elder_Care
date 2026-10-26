package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.m03.internal.bed.service.BedService;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m03.FakeBedAssets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class FR_011_BedCommandsIT extends StructureTestSupport {
    @Autowired
    BedService commands;

    @Autowired
    FakeBedAssets assets;

    private void suspend() {
        ReflectionTestUtils.invokeMethod(commands, "suspend", bed, Reason.of("Đóng tạm phòng để kiểm tra"));
    }

    private void reuse() {
        ReflectionTestUtils.invokeMethod(commands, "reuse", bed, Reason.of("Mở lại phòng đã kiểm tra"));
    }

    private String state() {
        return jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed);
    }

    @Test
    void FR_011_suspendAndReuseRequireReadinessAndKeepHistory() {
        suspend();
        assertThat(state()).isEqualTo("SUSPENDED");
        reuse();
        assertThat(state()).isEqualTo("AWAITING_READY");
        assertThat(jdbc.queryForObject("SELECT last_closed_allocation_id FROM bed WHERE id=?", Long.class, bed))
                .isNull();
        assertThat(jdbc.queryForObject(
                        "SELECT ready_deadline>awaiting_ready_since FROM bed WHERE id=?", Boolean.class, bed))
                .isTrue();
        assertThat(jdbc.queryForList(
                        "SELECT event FROM bed_state_history WHERE bed_id=? ORDER BY id", String.class, bed))
                .containsExactly("SUSPEND", "REUSE");
    }

    @Test
    void BR_M03_15_suspendRejectsAwaitingResidentTransfer() {
        assets.flag(bed, true);
        try {
            assertThatThrownBy(this::suspend)
                    .isInstanceOf(com.cambae.backend.common.error.ConditionNotMetException.class);
            assertThat(state()).isEqualTo("EMPTY");
        } finally {
            assets.flag(bed, false);
        }
    }

    @Test
    void FR_011_suspendRejectsFutureAllocation() {
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE','2099-01-01','BASIC','CURRENT')",
                TestUsers.nextId(),
                bed);
        assertThatThrownBy(this::suspend).isInstanceOf(com.cambae.backend.common.error.ConditionNotMetException.class);
        assertThat(state()).isEqualTo("EMPTY");
    }

    @Test
    void FR_011_suspendRequiresReason() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(commands, "suspend", bed, null))
                .isInstanceOf(com.cambae.backend.common.error.ReasonRequiredException.class);
        assertThat(state()).isEqualTo("EMPTY");
    }

    @Test
    void Q_261_reuseRejectsInactiveRoomAndRollsBack() {
        suspend();
        jdbc.update("UPDATE room SET status='INACTIVE' WHERE id=?", room);
        assertThatThrownBy(this::reuse).isInstanceOf(com.cambae.backend.common.error.BusinessException.class);
        assertThat(state()).isEqualTo("SUSPENDED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_state_history WHERE bed_id=?", Integer.class, bed))
                .isEqualTo(1);
    }

    @Test
    void FR_011_onlyManagerMaySuspend() {
        user.set(new TestUsers(jdbc, time).staff(Role.DOCTOR).user());
        assertThatThrownBy(this::suspend).isInstanceOf(com.cambae.backend.common.error.PermissionDeniedException.class);
        assertThat(state()).isEqualTo("EMPTY");
    }
}
