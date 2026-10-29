package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
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

    @Test
    void FR_012_holdRejectsOverlapAndRollsBack() {
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE',?,'BASIC','CURRENT')",
                TestUsers.nextId(),
                bed,
                java.sql.Timestamp.from(time.now().plusSeconds(1)));
        assertThatThrownBy(() -> hold(TestUsers.nextId(), TestUsers.nextId()))
                .isInstanceOf(com.eldercare.backend.common.error.ConditionNotMetException.class);
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("EMPTY");
    }

    @Test
    void FR_012_waitingHoldNeedsTheCallerTransaction() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                        holds(), "holdForWaitingRecord", 1L, 2L, bed, Instant.parse("2026-01-01T00:00:00Z")))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }

    @Test
    void FR_015_releaseAbsentResidentClosesAllocationAndRequiresReadiness() {
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'ACTIVE','2026-01-01','BASIC','CURRENT')",
                resident,
                bed);
        jdbc.update(
                "UPDATE bed SET state='HELD',hold_reason='ABSENT_RESIDENT',hold_resident_id=? WHERE id=?",
                resident,
                bed);
        tx.executeWithoutResult(s -> ReflectionTestUtils.invokeMethod(
                holds(),
                "releaseWhenAbsent",
                resident,
                time.now(),
                new com.eldercare.backend.administration.auditlog.api.Basis("TEST", null, null, null)));
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("AWAITING_READY");
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE resident_id=?", String.class, resident))
                .isEqualTo("CLOSED");
    }

    @Test
    void FR_015_extendingExpectedEndChangesOnlyTheActiveAllocation() {
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'ACTIVE','2026-01-01','BASIC','CURRENT')",
                resident,
                bed);
        Instant end = Instant.parse("2099-01-01T00:00:00Z");
        tx.executeWithoutResult(s -> ReflectionTestUtils.invokeMethod(holds(), "extendExpectedEnd", resident, end));
        assertThat(jdbc.queryForObject(
                        "SELECT expected_end IS NOT NULL FROM bed_allocation WHERE resident_id=?",
                        Boolean.class,
                        resident))
                .isTrue();
    }

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.roombedallocation.internal.bed.persistence.BedHoldStore holdStore;

    @Test
    @org.junit.jupiter.api.Timeout(20)
    void FR_014_concurrentCancellationIsIdempotentAfterWaitingForTheLock() throws Exception {
        long waiting = TestUsers.nextId();
        hold(waiting, TestUsers.nextId());
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        var firstRead = ThreadLocal.withInitial(() -> true);
        var target = org.springframework.test.util.AopTestUtils
                .<com.eldercare.backend.roombedallocation.internal.bed.persistence.BedHoldStore>getUltimateTargetObject(
                        holdStore);
        org.mockito.Mockito.doAnswer(invocation -> {
                    Object result = invocation.callRealMethod();
                    if (firstRead.get()) {
                        firstRead.set(false);
                        barrier.await(10, java.util.concurrent.TimeUnit.SECONDS);
                    }
                    return result;
                })
                .when(target)
                .waiting(waiting);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var one = pool.submit(() -> tx.executeWithoutResult(
                    s -> ReflectionTestUtils.invokeMethod(holds(), "cancelHold", waiting, Reason.of("Hủy hồ sơ chờ"))));
            var two = pool.submit(() -> tx.executeWithoutResult(
                    s -> ReflectionTestUtils.invokeMethod(holds(), "cancelHold", waiting, Reason.of("Hủy hồ sơ chờ"))));
            one.get(15, java.util.concurrent.TimeUnit.SECONDS);
            two.get(15, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM bed_state_history WHERE bed_id=? AND event='HOLD_CANCELLED'",
                            Integer.class,
                            bed))
                    .isEqualTo(1);
        } finally {
            pool.shutdownNow();
            org.mockito.Mockito.reset(target);
        }
    }

    @Test
    void FR_015_extensionOverlapIsAConditionFailureAndLeavesTheOldEnd() {
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,expected_end,checked_care_level,care_level_source) VALUES(?,?,'ACTIVE','2026-01-01','2099-01-10','BASIC','CURRENT')",
                resident,
                bed);
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE','2099-01-11','BASIC','CURRENT')",
                TestUsers.nextId(),
                bed);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> ReflectionTestUtils.invokeMethod(
                        holds(), "extendExpectedEnd", resident, Instant.parse("2099-01-12T00:00:00Z"))))
                .isInstanceOf(com.eldercare.backend.common.error.ConditionNotMetException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT expected_end='2099-01-10' FROM bed_allocation WHERE resident_id=?",
                        Boolean.class,
                        resident))
                .isTrue();
    }
}
