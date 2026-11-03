package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.incident.internal.governance.domain.ResidentMonitoringState;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** T007/S-03: khóa chung M05 cho cảnh báo, sự cố và các dấu theo dõi. */
class FR_030_MonitoringLockIT extends MySqlIntegrationTestBase {
    @Autowired
    MonitoringLock lock;

    @Autowired
    ResidentModuleStateApi states;

    @Autowired
    PlatformTransactionManager transactions;

    @Test
    void FR_030_requiresCallerTransaction() {
        long resident = TestUsers.nextId();
        assertThatThrownBy(() -> lock.lock(resident)).isInstanceOf(IllegalTransactionStateException.class);
        assertThat(states.get(ResidentMonitoringState.KIND, resident)).isEmpty();
    }

    @Test
    void FR_030_repeatedCommandsShareStateOnlyForTheirResident() {
        long resident = TestUsers.nextId();
        var tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> lock.lock(resident));
        tx.executeWithoutResult(status -> lock.lock(resident));
        assertThat(states.get(ResidentMonitoringState.KIND, resident)).contains(new ResidentMonitoringState());
        assertThat(states.get(ResidentMonitoringState.KIND, TestUsers.nextId())).isEmpty();
    }

    @Test
    void FR_030_initialStateRollsBackWithTheCaller() {
        long resident = TestUsers.nextId();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            lock.lock(resident);
            assertThat(states.get(ResidentMonitoringState.KIND, resident)).isPresent();
            status.setRollbackOnly();
        });
        assertThat(states.get(ResidentMonitoringState.KIND, resident)).isEmpty();
    }

    @Test
    void FR_030_secondCommandWaitsUntilTheFirstTransactionEnds() throws Exception {
        long resident = TestUsers.nextId();
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                lock.lock(resident);
                locked.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS))
                        throw new AssertionError("Chưa giải phóng transaction đầu");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }));
            try {
                assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
                var second = workers.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    secondStarted.countDown();
                    lock.lock(resident);
                }));
                assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> second.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                release.countDown();
                first.get(10, TimeUnit.SECONDS);
                second.get(10, TimeUnit.SECONDS);
            } finally {
                release.countDown();
            }
        }
    }
}
