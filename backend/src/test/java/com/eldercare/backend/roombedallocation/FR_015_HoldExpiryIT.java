package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

class FR_015_HoldExpiryIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @MockitoSpyBean
    FakeAllocationResidencySource source;

    private void run() {
        ReflectionTestUtils.invokeMethod(context.getBean("holdExpiryJob"), "runNow");
    }

    private long expired(String reason) {
        long resident = TestUsers.nextId();
        long waiting = TestUsers.nextId();
        jdbc.update(
                "UPDATE bed SET state='HELD',hold_reason=?,hold_resident_id=?,hold_waiting_record_id=?,hold_until='2000-01-01' WHERE id=?",
                reason,
                resident,
                reason.equals("WAITING_RECORD") ? waiting : null,
                bed);
        return waiting;
    }

    @Test
    void FR_015_waitingHoldExpiresOnceAndCalls004InTheSameTransaction() {
        long waiting = expired("WAITING_RECORD");
        run();
        run();
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("EMPTY");
        verify(source, times(1)).waitingRecordHoldExpired(waiting);
        assertThat(jdbc.queryForObject(
                        "SELECT executed_late FROM bed_state_history WHERE bed_id=?", Boolean.class, bed))
                .isTrue();
    }

    @Test
    void FR_015_absentHoldIsMarkedOnceWithoutReleasingTheBed() {
        expired("ABSENT_RESIDENT");
        run();
        run();
        assertThat(jdbc.queryForObject(
                        "SELECT state='HELD' AND hold_expiry_notified_at IS NOT NULL FROM bed WHERE id=?",
                        Boolean.class,
                        bed))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_state_history WHERE bed_id=?", Integer.class, bed))
                .isZero();
    }

    @Test
    void FR_015_callbackFailureRollsBackStateAndHistory() {
        long waiting = expired("WAITING_RECORD");
        doThrow(new IllegalStateException("Nguồn 004 lỗi"))
                .when(org.springframework.test.util.AopTestUtils.<FakeAllocationResidencySource>getUltimateTargetObject(
                        source))
                .waitingRecordHoldExpired(waiting);
        try {
            assertThatThrownBy(this::run).hasMessageContaining("Nguồn 004 lỗi");
            assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                    .isEqualTo("HELD");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_state_history WHERE bed_id=?", Integer.class, bed))
                    .isZero();
        } finally {
            reset(org.springframework.test.util.AopTestUtils.<FakeAllocationResidencySource>getUltimateTargetObject(
                    source));
            jdbc.update("UPDATE bed SET hold_until='2099-01-01' WHERE id=?", bed);
        }
    }
}
