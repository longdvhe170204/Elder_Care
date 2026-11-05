package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingResidentLock;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 013 K-03 (FR-011b, BR-M12-03; test đồng thời Trả và Kết thúc lưu trú): điều kiện {@code END_B_BELONGINGS} của lệnh Kết
 * thúc lưu trú đọc dưới khóa {@code do_gui_nct} nên chờ lệnh đang ghi trên đồ của người đó xong rồi mới đánh giá; bản xem
 * trước (transaction chỉ đọc) không khóa.
 */
class K_03_EndingVsReturnConcurrencyIT extends HandoverTestBase {

    @Autowired
    List<StatusTransitionParticipant> participants;

    @Autowired
    BelongingResidentLock residentLock;

    @Autowired
    PlatformTransactionManager transactionManager;

    private List<Condition> endConditions(long resident, boolean readOnly) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setReadOnly(readOnly);
        StatusTransitionParticipant participant = participants.stream()
                .filter(p -> p.getClass().getSimpleName().equals("BelongingTransitionParticipant"))
                .findFirst()
                .orElseThrow();
        return new TransactionTemplate(transactionManager, definition)
                .execute(status -> participant.checkConditions(new CommandContext(
                        resident,
                        ResidentStatus.IN_RESIDENCE,
                        StatusCommand.END_RESIDENCY,
                        null,
                        Instant.now(),
                        null)));
    }

    /** Giữ khóa {@code do_gui_nct} và trả đồ (chưa commit) tới khi {@code release} được mở. */
    private Future<?> returnInFlight(
            java.util.concurrent.ExecutorService pool,
            long resident,
            long belonging,
            CountDownLatch locked,
            CountDownLatch release) {
        return pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            residentLock.lock(resident);
            jdbc.update(
                    "UPDATE belonging SET status = 'RETURNED', holder_type = NULL, holder_id = NULL WHERE id = ?",
                    belonging);
            locked.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }

    @Test
    void endingCondition_waitsForReturnInFlight_andThenSeesItsResult() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> returning = returnInFlight(pool, resident, phone, locked, release);
            assertThat(locked.await(30, TimeUnit.SECONDS)).isTrue();

            Future<List<Condition>> ending = pool.submit(() -> endConditions(resident, false));
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> ending.get(1, TimeUnit.SECONDS))
                    .isInstanceOf(TimeoutException.class);

            release.countDown();
            returning.get(30, TimeUnit.SECONDS);
            assertThat(ending.get(30, TimeUnit.SECONDS)).isEmpty();
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void previewInReadOnlyTransaction_doesNotWait() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> returning = returnInFlight(pool, resident, phone, locked, release);
            assertThat(locked.await(30, TimeUnit.SECONDS)).isTrue();

            Future<List<Condition>> preview = pool.submit(() -> endConditions(resident, true));

            assertThat(preview.get(10, TimeUnit.SECONDS)).hasSize(1);
            release.countDown();
            returning.get(30, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }
}
