package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.billing.internal.period.domain.BillingResidentState;
import com.eldercare.backend.billing.internal.period.service.CostLock;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** T005/T007: khóa M11 và trạng thái qua API thật của A, gồm cạnh tranh khi chưa có dòng. */
class CostLockIT extends MySqlIntegrationTestBase {
    private static final Instant AT = Instant.parse("2026-11-04T01:00:00Z");

    @Autowired
    CostLock locks;

    @Autowired
    ResidentModuleStateApi states;

    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    JdbcTemplate jdbc;

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactions);
    }

    @Test
    void DBR_15_lockRequiresTheCallersTransaction() {
        long resident = TestUsers.nextId();
        assertThatThrownBy(() -> locks.resident(resident)).isInstanceOf(IllegalTransactionStateException.class);
        assertThat(states.get(BillingResidentState.KIND, resident)).isEmpty();
    }

    @Test
    void FR_036_firstLockCreatesExactlyOneM11StateThroughTheSharedApi() {
        long resident = TestUsers.nextId();
        transaction().executeWithoutResult(status -> {
            assertThat(locks.resident(resident).finalCostCompletedFrom()).isNull();
            states.save(BillingResidentState.KIND, resident, new BillingResidentState(AT));
            assertThat(locks.resident(resident).finalCostCompletedFrom()).isEqualTo(AT);
        });
        assertThat(states.get(BillingResidentState.KIND, resident)).contains(new BillingResidentState(AT));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM resident_module_state WHERE module='M11' AND resident_id=?",
                        Integer.class,
                        resident))
                .isEqualTo(1);
    }

    @Test
    void DBR_15_rollbackAlsoRemovesTheNewStateAndTechnicalLock() {
        long resident = TestUsers.nextId();
        transaction().executeWithoutResult(status -> {
            locks.resident(resident);
            states.save(BillingResidentState.KIND, resident, new BillingResidentState(AT));
            status.setRollbackOnly();
        });
        assertThat(states.get(BillingResidentState.KIND, resident)).isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM technical_lock WHERE lock_key=?",
                        Integer.class,
                        "M11.COST_RESIDENT:" + resident))
                .isZero();
    }

    @Test
    void DBR_15_twoFirstCommandsReadEachOthersCommittedStateInsteadOfLosingAnUpdate() throws Exception {
        long resident = TestUsers.nextId();
        var start = new CyclicBarrier(2);
        Callable<Optional<Instant>> command = () -> {
            start.await(10, TimeUnit.SECONDS);
            return transaction().execute(status -> {
                var before = Optional.ofNullable(locks.resident(resident).finalCostCompletedFrom());
                states.save(BillingResidentState.KIND, resident, new BillingResidentState(AT));
                return before;
            });
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(command);
            var second = executor.submit(command);
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(Optional.empty(), Optional.of(AT));
        }
    }

    @Test
    void DBR_17_statementLockRequiresTheCorrectResidentAndKeepsExistingState() {
        long resident = TestUsers.nextId();
        long statement = statement(resident);
        transaction().executeWithoutResult(status -> {
            locks.resident(resident);
            states.save(BillingResidentState.KIND, resident, new BillingResidentState(AT));
            assertThat(locks.statement(resident, statement).finalCostCompletedFrom())
                    .isEqualTo(AT);
        });
        assertThatThrownBy(() ->
                        transaction().executeWithoutResult(status -> locks.statement(TestUsers.nextId(), statement)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void DBR_17_unknownStatementDoesNotCommitAPartialResidentState() {
        long resident = TestUsers.nextId();
        assertThatThrownBy(() ->
                        transaction().executeWithoutResult(status -> locks.statement(resident, TestUsers.nextId())))
                .isInstanceOf(NotFoundException.class);
        assertThat(states.get(BillingResidentState.KIND, resident)).isEmpty();
    }

    private long statement(long resident) {
        long period = TestUsers.nextId(), statement = TestUsers.nextId();
        long seed = Math.floorMod(resident, 72000);
        var month = YearMonth.of(2000 + (int) seed / 12, 1 + (int) seed % 12);
        jdbc.update(
                """
                INSERT INTO cost_statement
                  (id,kind,period_month,period_starts_on,period_ends_on,unique_key,created_at)
                VALUES (?,'PERIOD',?,?,?,?,?)
                """,
                period,
                month.toString(),
                month.atDay(1),
                month.atEndOfMonth(),
                "PERIOD:" + month,
                Timestamp.from(AT));
        jdbc.update(
                """
                INSERT INTO cost_statement
                  (id,kind,period_id,resident_id,statement_type,unique_key,created_at)
                VALUES (?,'STATEMENT',?,?,'REGULAR',?,?)
                """,
                statement,
                period,
                resident,
                "STATEMENT:" + period + ":" + resident + ":REGULAR",
                Timestamp.from(AT));
        return statement;
    }
}
