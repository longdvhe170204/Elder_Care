package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

class FR_012_BedHoldsIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @Autowired
    TransactionTemplate tx;

    private Object holds() {
        return context.getBean("bedHoldService");
    }

    private void hold(long waiting, long resident) {
        tx.executeWithoutResult(s ->
                ReflectionTestUtils.invokeMethod(holds(), "holdForWaitingRecord", waiting, resident, bed, time.now()));
    }

    @Test
    void FR_012_waitingHoldGetsConfiguredDeadlineAndCancellationIsIdempotent() {
        long waiting = TestUsers.nextId(), resident = TestUsers.nextId();
        hold(waiting, resident);
        assertThat(jdbc.queryForObject(
                        "SELECT state='HELD' AND hold_until IS NOT NULL FROM bed WHERE id=?", Boolean.class, bed))
                .isTrue();
        tx.executeWithoutResult(
                s -> ReflectionTestUtils.invokeMethod(holds(), "cancelHold", waiting, Reason.of("Hủy hồ sơ chờ")));
        tx.executeWithoutResult(
                s -> ReflectionTestUtils.invokeMethod(holds(), "cancelHold", waiting, Reason.of("Hủy hồ sơ chờ")));
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("EMPTY");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_state_history WHERE bed_id=?", Integer.class, bed))
                .isEqualTo(2);
    }

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.roombedallocation.internal.bed.persistence.BedHoldStore holdStore;
}
