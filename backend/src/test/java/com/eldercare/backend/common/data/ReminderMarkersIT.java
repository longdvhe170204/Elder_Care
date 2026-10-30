package com.eldercare.backend.common.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/** {@link ReminderMarkers}, {@link TechnicalLocks} trên {@code reminder_marker}, {@code technical_lock} (000 T108, R-15). */
class ReminderMarkersIT extends MySqlIntegrationTestBase {

    @Autowired
    ReminderMarkers markers;

    @Autowired
    TechnicalLocks locks;

    @Autowired
    TransactionTemplate transaction;

    @Test
    void mark_isTrueOnlyTheFirstTime() {
        long entity = TestUsers.nextId();

        Boolean first =
                transaction.execute(s -> markers.mark("M15.TEST", "M15.TEST_ENTITY", entity, "d1", Map.of("a", 1)));
        Boolean second = transaction.execute(s -> markers.mark("M15.TEST", "M15.TEST_ENTITY", entity, "d1", null));

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(markers.isMarked("M15.TEST", "M15.TEST_ENTITY", entity, "d1"))
                .isTrue();
        assertThat(markers.isMarked("M15.TEST", "M15.TEST_ENTITY", entity, "d2"))
                .isFalse();
    }

    @Test
    void lock_requiresTransaction() {
        assertThatThrownBy(() -> locks.lock("M15.TEST:" + TestUsers.nextId()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void lock_serializesTransactionsOnSameKey() throws Exception {
        String key = "M15.TEST:" + TestUsers.nextId();
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> transaction.executeWithoutResult(s -> {
                locks.lock(key);
                held.countDown();
                await(release);
            }));
            assertThat(held.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> second = pool.submit(() -> transaction.executeWithoutResult(s -> locks.lock(key)));

            Thread.sleep(300);
            assertThat(second.isDone()).as("lệnh thứ hai chờ khóa").isFalse();
            release.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
