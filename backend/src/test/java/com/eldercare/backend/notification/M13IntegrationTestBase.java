package com.eldercare.backend.notification;

import static org.awaitility.Awaitility.await;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.notification.FakeM13Config;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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

    /**
     * Job giải phóng giờ yên tĩnh quét mọi thông báo đang giữ: đẩy dòng sót của test khác (ví dụ thông báo người thân của
     * 012 chạy ban đêm theo giờ thật) ra xa để job trong test chỉ gặp dòng của chính nó (CLAUDE.md, mẫu
     * {@code CallReminderTestBase}).
     */
    @BeforeEach
    void isolateHeldNotices() {
        jdbc.update("UPDATE notification SET scheduled_send_at = '2099-01-01 00:00:00'"
                + " WHERE status = 'HELD_FOR_QUIET_HOURS'");
    }

    /**
     * Gọi ngay trước job giải phóng: đẩy ra xa dòng đang giữ không thuộc các yêu cầu {@code requestIds} của test.
     * {@link #isolateHeldNotices()} chỉ chạy đầu test; dòng sinh sau đó (job của context khác trong cùng JVM chạy theo giờ
     * thật, người cao tuổi không có trong bản giả của context này) vẫn tới hạn trước mốc giờ test tua tới.
     */
    void isolateHeldNoticesExcept(long... requestIds) {
        jdbc.update(
                "UPDATE notification SET scheduled_send_at = '2099-01-01 00:00:00' WHERE status = 'HELD_FOR_QUIET_HOURS'"
                        + " AND request_id NOT IN (" + String.join(", ", Collections.nCopies(requestIds.length, "?"))
                        + ")",
                Arrays.stream(requestIds).boxed().toArray());
    }

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
