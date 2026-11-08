package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.roombedallocation.internal.bed.service.BedService;
import com.eldercare.backend.support.fake.roombedallocation.FakeBedAssets;
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
}
