package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.billing.internal.period.domain.BillingResidentState;
import com.eldercare.backend.billing.internal.period.service.CostLock;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
