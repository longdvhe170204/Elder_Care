package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.health.internal.governance.domain.ResidentVitalSignState;
import com.eldercare.backend.health.internal.governance.service.VitalSignLock;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** T007/S-03: khóa trạng thái M06 giữ tới hết transaction, kể cả lần đầu chưa có dòng. */
class FR_024_VitalSignLockIT extends MySqlIntegrationTestBase {
    @Autowired
    VitalSignLock lock;

    @Autowired
    ResidentModuleStateApi states;

    @Autowired
    PlatformTransactionManager transactions;

    @Test
    void FR_024_requiresCallerTransaction() {
        long resident = TestUsers.nextId();
        assertThatThrownBy(() -> lock.lock(resident)).isInstanceOf(IllegalTransactionStateException.class);
        assertThat(states.get(ResidentVitalSignState.KIND, resident)).isEmpty();
    }

    @Test
    void FR_025_relockingPreservesTheMissingThresholdReminder() {
        long resident = TestUsers.nextId();
        var at = Instant.parse("2026-11-02T00:00:00Z");
        tx().executeWithoutResult(status -> {
            lock.lock(resident);
            states.save(ResidentVitalSignState.KIND, resident, new ResidentVitalSignState(at));
        });
        tx().executeWithoutResult(status -> lock.lock(resident));
        assertThat(states.get(ResidentVitalSignState.KIND, resident)).contains(new ResidentVitalSignState(at));
        assertThat(states.get(ResidentVitalSignState.KIND, TestUsers.nextId())).isEmpty();
    }

    @Test
    void FR_024_initialStateRollsBackWithTheCaller() {
        long resident = TestUsers.nextId();
        tx().executeWithoutResult(status -> {
            lock.lock(resident);
            assertThat(states.get(ResidentVitalSignState.KIND, resident)).isPresent();
            status.setRollbackOnly();
        });
        assertThat(states.get(ResidentVitalSignState.KIND, resident)).isEmpty();
    }

    @Test
    void FR_024_tenConcurrentFirstCommandsDoNotLoseStateUpdates() throws Exception {
        long resident = TestUsers.nextId();
        var start = new CountDownLatch(1);
        var base = Instant.parse("2026-11-02T00:00:00Z");
        try (var workers = Executors.newFixedThreadPool(10)) {
            var results = new ArrayList<Future<?>>();
            for (int i = 0; i < 10; i++) {
                results.add(workers.submit(() -> {
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Chưa mở cổng chạy đồng thời");
                    tx().executeWithoutResult(status -> {
                        lock.lock(resident);
                        var current = states.get(ResidentVitalSignState.KIND, resident)
                                .orElseThrow();
                        var previous = current.missingThresholdRemindedAt();
                        states.save(
                                ResidentVitalSignState.KIND,
                                resident,
                                new ResidentVitalSignState((previous == null ? base : previous).plusSeconds(1)));
                    });
                    return null;
                }));
            }
            start.countDown();
            for (var result : results) result.get(30, TimeUnit.SECONDS);
        }
        assertThat(states.get(ResidentVitalSignState.KIND, resident))
                .contains(new ResidentVitalSignState(base.plusSeconds(10)));
    }

    private TransactionTemplate tx() {
        return new TransactionTemplate(transactions);
    }
}
