package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.notification.internal.delivery.service.DeliveryDispatcher;
import com.eldercare.backend.notification.internal.persistence.NotificationStore;
import com.eldercare.backend.notification.internal.persistence.NotificationStore.NewNotification;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * FR-017, FR-018, NFR-03, research B-03, B-05 (009 T008): lần gửi chạy song song sau commit; mỗi kết quả một dòng;
 * kênh trong ứng dụng là đã gửi khi thông báo vào hộp, đồng thời đẩy FCM tới mọi thiết bị còn hiệu lực; token lỗi bị vô
 * hiệu; Khẩn cấp có hàng riêng, không chờ sau thông báo thường đang dồn.
 */
@Import(NFR_03_DeliveryDispatcherIT.PushConfig.class)
class NFR_03_DeliveryDispatcherIT extends M13IntegrationTestBase {

    @TestConfiguration
    static class PushConfig {
        @Bean
        @Primary
        FakePushGateway fakePushGateway() {
            return new FakePushGateway();
        }
    }

    private static final AtomicLong ACCOUNTS = new AtomicLong(900_000 + System.nanoTime() % 1000 * 100);

    @Autowired
    FakePushGateway push;

    @Autowired
    DeliveryDispatcher dispatcher;

    @Autowired
    NotificationStore notifications;

    @AfterEach
    void reset() {
        push.release.countDown();
        push.reset();
    }

    private long requestId(NotificationLevel level) {
        String key = UUID.randomUUID().toString();
        jdbc.update(
                "INSERT INTO notification_request (source_module, event_type, source_record_type, source_record_id,"
                        + " event_key, level, title, received_at, intake_result) VALUES ('M05', 'TEST', 'M05.INCIDENT',"
                        + " ?, ?, ?, 'Sự cố mới', UTC_TIMESTAMP(6), 'VALID')",
                key,
                key,
                level.name());
        return jdbc.queryForObject("SELECT id FROM notification_request WHERE event_key = ?", Long.class, key);
    }

    /** Thông báo trong ứng dụng cho một tài khoản có các token đã cho; trả id lần gửi. */
    private long inAppNotification(NotificationLevel level, String... tokens) {
        long account = ACCOUNTS.incrementAndGet();
        for (String token : tokens) {
            jdbc.update(
                    "INSERT INTO push_device (account_id, device_id, app, fcm_token, updated_at)"
                            + " VALUES (?, ?, 'STAFF_APP', ?, UTC_TIMESTAMP(6))",
                    account,
                    UUID.randomUUID().toString(),
                    token);
        }
        long[] attempt = new long[1];
        tx.executeWithoutResult(s -> {
            long id = notifications.insert(new NewNotification(
                    requestId(level),
                    RecipientType.STAFF,
                    account,
                    account,
                    List.of("TEST"),
                    Set.of(),
                    false,
                    level,
                    Instant.now(),
                    null));
            notifications.markSent(id, Instant.now());
            attempt[0] = notifications.planAttempt(id, "IN_APP", null, Instant.now());
        });
        return attempt[0];
    }

    private String result(long attemptId) {
        List<String> r =
                jdbc.queryForList("SELECT result FROM delivery_result WHERE attempt_id = ?", String.class, attemptId);
        return r.isEmpty() ? null : r.getFirst();
    }

    private String status(long attemptId) {
        return jdbc.queryForObject(
                "SELECT s.status FROM notification_state s JOIN delivery_attempt a ON a.notification_id ="
                        + " s.notification_id WHERE a.id = ?",
                String.class,
                attemptId);
    }

    @Test
    void FR_018_inApp_isSent_andPushedToEveryValidDevice_invalidTokenIsDisabled() {
        String good = "tok-" + UUID.randomUUID();
        String bad = "tok-" + UUID.randomUUID();
        push.invalid.add(bad);
        long attempt = inAppNotification(NotificationLevel.MEDIUM, good, bad);

        dispatcher.dispatch(List.of(attempt));

        await().atMost(Duration.ofSeconds(10)).until(() -> result(attempt) != null);
        assertThat(result(attempt)).isEqualTo("SENT");
        assertThat(status(attempt)).isEqualTo("SENT");
        assertThat(push.pushesTo(good)).hasSize(1);
        assertThat(push.pushesTo(good).getFirst().message().title()).isEqualTo("Sự cố mới");
        await().atMost(Duration.ofSeconds(5))
                .until(() -> jdbc.queryForObject(
                                "SELECT COUNT(*) FROM push_device WHERE fcm_token = ? AND invalidated_at IS NOT NULL",
                                Integer.class,
                                bad)
                        == 1);
    }

    @Test
    void FR_018_sameAttemptDispatchedTwice_hasOneResult() {
        long attempt = inAppNotification(NotificationLevel.LOW);

        dispatcher.dispatch(List.of(attempt, attempt));

        await().atMost(Duration.ofSeconds(10)).until(() -> result(attempt) != null);
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(3)).until(() -> true);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM delivery_result WHERE attempt_id = ?", Integer.class, attempt))
                .isEqualTo(1);
    }

    @Test
    void FR_017_dispatchInTransaction_runsOnlyAfterCommit() {
        long attempt = inAppNotification(NotificationLevel.LOW);

        tx.executeWithoutResult(s -> {
            dispatcher.dispatch(List.of(attempt));
            s.setRollbackOnly();
        });
        long committed = inAppNotification(NotificationLevel.LOW);
        tx.executeWithoutResult(s -> dispatcher.dispatch(List.of(committed)));

        await().atMost(Duration.ofSeconds(10)).until(() -> result(committed) != null);
        assertThat(result(attempt)).isNull();
    }

    @Test
    void NFR_03_urgent_isNotQueuedBehindBacklog() {
        push.release = new CountDownLatch(1);
        List<Long> backlog = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            backlog.add(inAppNotification(NotificationLevel.LOW, "slow-" + UUID.randomUUID()));
        }
        long urgent = inAppNotification(NotificationLevel.URGENT, "tok-" + UUID.randomUUID());

        dispatcher.dispatch(backlog);
        dispatcher.dispatch(List.of(urgent));

        await().atMost(Duration.ofSeconds(5)).until(() -> result(urgent) != null);
        assertThat(backlog).allSatisfy(id -> assertThat(result(id)).isNull());
        push.release.countDown();
        await().atMost(Duration.ofSeconds(15)).until(() -> backlog.stream().allMatch(id -> result(id) != null));
    }
}
