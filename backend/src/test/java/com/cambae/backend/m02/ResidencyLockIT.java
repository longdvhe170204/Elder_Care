package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 004 T006 (research T-03; DBR-06): mọi lệnh 004 trên cùng người khóa dòng {@code resident_residency} nên nối tiếp
 * nhau; dòng được tạo ở lần khóa đầu.
 */
class ResidencyLockIT extends MySqlIntegrationTestBase {

    @Autowired
    ResidencyLock lock;

    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void T_03_firstLock_createsRow_andLockIsRepeatable() {
        TransactionTemplate tx = new TransactionTemplate(transactions);

        tx.executeWithoutResult(status -> lock.lock(920001));
        tx.executeWithoutResult(status -> lock.lock(920001));

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM resident_residency WHERE resident_id = 920001", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void T_03_lockWithoutTransaction_isRejected() {
        assertThatThrownBy(() -> lock.lock(920002)).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void T_03_secondCommandOnSameResident_waitsUntilFirstCommits() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactions);
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        CompletableFuture<Void> first = CompletableFuture.runAsync(() -> tx.executeWithoutResult(status -> {
            lock.lock(920003);
            holding.countDown();
            await(release);
        }));
        assertThat(holding.await(10, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<Void> second =
                CompletableFuture.runAsync(() -> tx.executeWithoutResult(status -> lock.lock(920003)));
        Thread.sleep(500);
        assertThat(second).isNotDone();

        release.countDown();
        first.get(10, TimeUnit.SECONDS);
        second.get(10, TimeUnit.SECONDS);
    }

    @Test
    void T_03_differentResidents_doNotBlockEachOther() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactions);
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        CompletableFuture<Void> first = CompletableFuture.runAsync(() -> tx.executeWithoutResult(status -> {
            lock.lock(920004);
            holding.countDown();
            await(release);
        }));
        assertThat(holding.await(10, TimeUnit.SECONDS)).isTrue();

        CompletableFuture.runAsync(() -> tx.executeWithoutResult(status -> lock.lock(920005)))
                .get(10, TimeUnit.SECONDS);

        release.countDown();
        first.get(10, TimeUnit.SECONDS);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
