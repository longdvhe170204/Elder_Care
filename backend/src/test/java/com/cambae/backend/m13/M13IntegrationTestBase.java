package com.cambae.backend.m13;

import static org.awaitility.Awaitility.await;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m13.FakeM13Config;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** Lớp cơ sở test tích hợp của 009: phát sự kiện nguồn trong transaction như một lệnh nghiệp vụ, rồi chờ M13 xử lý. */
@Import(FakeM13Config.class)
abstract class M13IntegrationTestBase extends MySqlIntegrationTestBase {

    /** Sự kiện miền giả của một module nguồn. */
    record SampleSourceEvent(List<NotificationRequest> requests) implements HasNotificationRequests {
        @Override
        public List<NotificationRequest> notificationRequests() {
            return requests;
        }
    }

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    /** Lệnh nguồn: ghi một bản ghi nghiệp vụ mẫu và phát sự kiện trong cùng transaction. */
    void sourceCommand(String note, HasNotificationRequests event) {
        tx.executeWithoutResult(s -> {
            jdbc.update(
                    "INSERT INTO sample_office_record (occurred_at, actor_id, note) VALUES (UTC_TIMESTAMP(6), 1, ?)",
                    note);
            events.publishEvent(event);
        });
    }

    /** Chờ có đúng {@code count} yêu cầu với khóa sự kiện này. */
    List<Map<String, Object>> awaitRequests(String eventKey, int count) {
        await().atMost(Duration.ofSeconds(10)).until(() -> requests(eventKey).size() == count);
        return requests(eventKey);
    }

    List<Map<String, Object>> requests(String eventKey) {
        return jdbc.queryForList("SELECT * FROM notification_request WHERE event_key = ? ORDER BY id", eventKey);
    }
}
