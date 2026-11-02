package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.internal.delivery.persistence.DeliveryStore;
import com.eldercare.backend.notification.internal.delivery.persistence.SmsWindowStore;
import com.eldercare.backend.notification.internal.directcontact.persistence.DirectContactStore;
import com.eldercare.backend.notification.internal.inbox.domain.InboxFilter;
import com.eldercare.backend.notification.internal.inbox.persistence.InboxStore;
import com.eldercare.backend.notification.internal.lookup.persistence.LookupStore;
import com.eldercare.backend.notification.internal.quiethours.persistence.HeldNotificationStore;
import com.eldercare.backend.notification.internal.urgent.persistence.CallReminderStore;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * 009 T044, SC-001, NFR-03: đo lại sau gộp bảng T043 (job gửi, hộp thông báo, lượt nhắc gọi đọc {@code record_event}
 * bằng phép nối) trên dữ liệu một năm theo NFR-01: 365.000 yêu cầu, 1.095.000 thông báo (3.000 mỗi ngày) của 100 nhân
 * viên, 600 người thân, khoảng 5 triệu dòng M13 và 1 triệu dòng của module khác ở {@code record_event}.
 *
 * <p>Không chạy trong {@code verify}: sinh dữ liệu mất vài phút và làm chậm test quét toàn bảng. Chạy riêng trên schema
 * đuôi {@code _perf}: {@code TEST_DB_URL=jdbc:mysql://localhost:3306/eldercare_perf ./mvnw test-compile
 * failsafe:integration-test failsafe:verify -Dit.test=SC_001_UrgentConcurrencyIT -Deldercare.perf=true}.
 */
@EnabledIfSystemProperty(named = "eldercare.perf", matches = "true")
@Import(SC_001_UrgentConcurrencyIT.PushConfig.class)
class SC_001_UrgentConcurrencyIT extends M13IntegrationTestBase {

    @TestConfiguration
    static class PushConfig {
        @Bean
        @Primary
        FakePushGateway fakePushGateway() {
            return new FakePushGateway();
        }
    }

    static final int REQUESTS = 365_000;
    static final int NOTIFICATIONS = REQUESTS * 3;
    static final int OTHER_MODULE_EVENTS = 1_000_000;
    static final int CHUNK = 100_000;
    /** Dòng {@code record_event} dành cho mỗi thông báo: sự kiện gửi, lần gửi, kết quả trong ứng dụng; SMS; xem, xác nhận. */
    static final int SLOTS = 7;

    static final long STAFF_BASE = 50_000_000;
    static final long FAMILY_BASE = 51_000_000;
    static final long RESIDENT_BASE = 60_000_000;

    static long requestBase;
    static long notificationBase;
    static long eventBase;
    static boolean seeded;

    @Autowired
    FakePushGateway push;

    @Autowired
    BusinessTime time;

    @Autowired
    DeliveryStore deliveries;

    @Autowired
    InboxStore inbox;

    @Autowired
    CallReminderStore reminders;

    @Autowired
    DirectContactStore directContacts;

    @Autowired
    LookupStore lookup;

    @Autowired
    SmsWindowStore smsWindows;

    @Autowired
    HeldNotificationStore held;

    /** Sinh một lần mỗi JVM, sau khi context đã chạy Flyway. */
    @BeforeEach
    void seedOneYear() throws SQLException {
        if (seeded) {
            return;
        }
        seeded = true;
        try (Connection c = adminConnection();
                Statement s = c.createStatement()) {
            assertThat(c.getCatalog())
                    .as("chỉ sinh dữ liệu trên schema đuôi _perf")
                    .endsWith("_perf");
            long started = System.nanoTime();
            requestBase = max(s, "notification_request");
            notificationBase = max(s, "notification");
            eventBase = max(s, "record_event");
            // Khoảng 3 GB dữ liệu mỗi lần chạy: không ghi binlog (đầy ổ máy chạy test)
            s.execute("SET SESSION sql_log_bin = 0");
            s.execute("SET @t = UTC_TIMESTAMP(6) - INTERVAL 1 HOUR");
            s.execute("CREATE TEMPORARY TABLE perf_seq (n INT NOT NULL PRIMARY KEY)");
            s.execute("INSERT INTO perf_seq WITH d AS (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL"
                    + " SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL"
                    + " SELECT 8 UNION ALL SELECT 9) SELECT a.n + 10 * b.n + 100 * c.n + 1000 * e.n + 10000 * f.n"
                    + " FROM d a, d b, d c, d e, d f");

            for (int from = 0; from < REQUESTS; from += CHUNK) {
                s.execute("INSERT INTO notification_request (id, source_module, event_type, source_group,"
                        + " source_record_type, source_record_id, event_key, level, resident_id, floor_area_id,"
                        + " short_content, title, occurred_at, received_at, intake_result, contents, recipient_groups)"
                        + " SELECT " + requestBase + " + r + 1, 'M05', 'PERF_EVENT', 'OTHER', 'M05.INCIDENT',"
                        + " CONCAT('perf-', r), CONCAT('perf-', r), " + level("r") + ", " + RESIDENT_BASE
                        + " + r % 300, " + RESIDENT_BASE + " + r % 6, 'Nội dung ngắn', 'Sự kiện', " + at("r") + ", "
                        + at("r") + ", 'VALID', JSON_ARRAY(JSON_OBJECT('seq', 1, 'informationType', 'GENERAL',"
                        + " 'content', 'Nội dung')), JSON_ARRAY(JSON_OBJECT('seq', 1, 'groupCode', 'Staff',"
                        + " 'params', JSON_OBJECT(), 'outcome', 'HAS_RECIPIENTS', 'substitution', NULL,"
                        + " 'substituteGroup', NULL)) FROM (SELECT " + from + " + n AS r FROM perf_seq WHERE "
                        + from + " + n < " + REQUESTS + ") t");
            }
            for (int from = 0; from < NOTIFICATIONS; from += CHUNK) {
                String rows = " FROM (SELECT i, i DIV 3 AS r, i % 3 AS j FROM (SELECT " + from
                        + " + n AS i FROM perf_seq" + " WHERE " + from + " + n < " + NOTIFICATIONS + ") u) t";
                String recipient = "CASE j WHEN 0 THEN " + STAFF_BASE + " + r % 100 WHEN 1 THEN " + FAMILY_BASE
                        + " + r % 600 ELSE " + STAFF_BASE + " + (7 * r + 3) % 100 END";
                s.execute("INSERT INTO notification (id, request_id, recipient_type, recipient_id, account_id, basis,"
                        + " level, created_at, status, sent_at, viewed_at, acknowledged_at, acknowledgement_method,"
                        + " resident_id) SELECT " + notificationBase + " + i + 1, " + requestBase + " + r + 1,"
                        + " IF(j = 1, 'FAMILY_MEMBER', 'STAFF'), " + recipient + ", " + recipient
                        + ", JSON_ARRAY('Staff:perf'), " + level("r") + ", " + at("r") + ", " + status() + ", "
                        + at("r") + " + INTERVAL 1 SECOND, IF(" + status() + " = 'SENT', NULL, " + at("r")
                        + " + INTERVAL 5 MINUTE), IF(r % 20 = 0, " + at("r") + " + INTERVAL 2 MINUTE, NULL),"
                        + " IF(r % 20 = 0, 'IN_APP', NULL), " + RESIDENT_BASE + " + r % 300" + rows);
                String nid = notificationBase + " + i + 1";
                String sent = at("r") + " + INTERVAL 1 SECOND";
                String event = "INSERT INTO record_event (id, entity_type, entity_id, module, kind, content, actor_id,"
                        + " by_system, occurred_at, confirmed_at, unique_key) SELECT ";
                s.execute(event + id(0) + ", 'M13.NOTIFICATION', " + nid + ", 'M13', 'M13.NOTIFICATION_EVENT',"
                        + " JSON_OBJECT('type', 'SEND', 'reason', NULL, 'scheduledSendAt', NULL,"
                        + " 'acknowledgementMethod', NULL), NULL, TRUE, " + sent + ", " + sent
                        + ", CONCAT('M13.NE:', " + nid + ", ':SEND')" + rows);
                s.execute(event + id(1) + ", 'M13.NOTIFICATION', " + nid + ", 'M13', 'M13.DELIVERY_ATTEMPT',"
                        + " JSON_OBJECT('address', NULL, 'channel', 'IN_APP', 'content', NULL, 'attemptNo', 1,"
                        + " 'batchWindowId', NULL), NULL, TRUE, " + sent + ", " + sent + ", CONCAT('M13.DA:', " + nid
                        + ", ':IN_APP:1')" + rows);
                s.execute(event + id(2) + ", 'M13.DELIVERY_ATTEMPT', " + id(1) + ", 'M13', 'M13.DELIVERY_RESULT',"
                        + " JSON_OBJECT('result', 'SENT', 'reason', NULL, 'digestAttemptId', NULL), NULL, TRUE, " + sent
                        + ", " + sent + ", CONCAT('M13.DR:', " + id(1) + ")" + rows);
                s.execute(event + id(3) + ", 'M13.NOTIFICATION', " + nid + ", 'M13', 'M13.DELIVERY_ATTEMPT',"
                        + " JSON_OBJECT('address', '+84900000000', 'channel', 'SMS', 'content', 'Nội dung',"
                        + " 'attemptNo', 1, 'batchWindowId', NULL), NULL, TRUE, " + sent + ", " + sent
                        + ", CONCAT('M13.DA:', " + nid + ", ':SMS:1')" + rows + " WHERE j = 1");
                s.execute(event + id(4) + ", 'M13.DELIVERY_ATTEMPT', " + id(3) + ", 'M13', 'M13.DELIVERY_RESULT',"
                        + " IF(r % 25 = 0, JSON_OBJECT('result', 'FAILED', 'reason', 'PERMANENT:21614',"
                        + " 'digestAttemptId', NULL), JSON_OBJECT('result', 'SENT', 'reason', NULL, 'digestAttemptId',"
                        + " NULL)), NULL, TRUE, " + sent + ", " + sent + ", CONCAT('M13.DR:', " + id(3) + ")" + rows
                        + " WHERE j = 1");
                s.execute(event + id(5) + ", 'M13.NOTIFICATION', " + nid + ", 'M13', 'M13.NOTIFICATION_EVENT',"
                        + " JSON_OBJECT('type', 'VIEW', 'reason', NULL, 'scheduledSendAt', NULL,"
                        + " 'acknowledgementMethod', NULL), " + recipient + ", FALSE, " + at("r")
                        + " + INTERVAL 5 MINUTE, " + at("r") + " + INTERVAL 5 MINUTE, CONCAT('M13.NE:', " + nid
                        + ", ':VIEW')" + rows + " WHERE " + status() + " <> 'SENT'");
                s.execute(event + id(6) + ", 'M13.NOTIFICATION', " + nid + ", 'M13', 'M13.NOTIFICATION_EVENT',"
                        + " JSON_OBJECT('type', 'ACKNOWLEDGE', 'reason', NULL, 'scheduledSendAt', NULL,"
                        + " 'acknowledgementMethod', 'IN_APP'), " + recipient + ", FALSE, " + at("r")
                        + " + INTERVAL 2 MINUTE, " + at("r") + " + INTERVAL 2 MINUTE, CONCAT('M13.NE:', " + nid
                        + ", ':ACKNOWLEDGE')" + rows + " WHERE r % 20 = 0");
            }
            long otherBase = eventBase + (long) NOTIFICATIONS * SLOTS;
            for (int from = 0; from < OTHER_MODULE_EVENTS; from += CHUNK) {
                s.execute("INSERT INTO record_event (id, entity_type, entity_id, module, kind, content, actor_id,"
                        + " by_system, occurred_at, confirmed_at) SELECT " + otherBase + " + k + 1, 'M01.PERF_ITEM', k"
                        + " DIV 5 + 1, 'M01', 'M01.PERF_NOTE', JSON_OBJECT('note', 'Ghi chú'), 1, FALSE, "
                        + at("k DIV 3")
                        + ", " + at("k DIV 3") + " FROM (SELECT " + from + " + n AS k FROM perf_seq WHERE " + from
                        + " + n < " + OTHER_MODULE_EVENTS + ") t");
            }
            s.execute("ANALYZE TABLE notification_request, notification, record_event");
            System.out.printf(
                    "[T044] sinh dữ liệu một năm: %d yêu cầu, %d thông báo, %d dòng record_event trong %d giây%n",
                    count(s, "notification_request"),
                    count(s, "notification"),
                    count(s, "record_event"),
                    (System.nanoTime() - started) / 1_000_000_000);
        }
    }

    /** Một ngày 1.000 yêu cầu trong một năm, id tăng theo thời gian như dữ liệu thật; 5% Khẩn cấp, 35% Trung bình. */
    private static String at(String r) {
        String age = "(" + (REQUESTS - 1) + " - (" + r + "))";
        return "(@t - INTERVAL (" + age + " DIV 1000) DAY - INTERVAL (" + age + " * 86 % 86400) SECOND)";
    }

    private static String level(String r) {
        return "CASE WHEN " + r + " % 20 = 0 THEN 'URGENT' WHEN " + r + " % 20 <= 7 THEN 'MEDIUM' ELSE 'LOW' END";
    }

    /** Khẩn cấp đã xác nhận (không sinh lượt nhắc gọi); còn lại 70% đã xem, rải đều cho mọi người nhận. */
    private static String status() {
        return "IF(r % 20 = 0, 'ACKNOWLEDGED', IF((r DIV 7 + i) % 10 < 7, 'VIEWED', 'SENT'))";
    }

    private static String id(int slot) {
        return "(" + eventBase + " + " + SLOTS + " * i + " + (slot + 1) + ")";
    }

    private static long max(Statement s, String table) throws SQLException {
        try (var rs = s.executeQuery("SELECT COALESCE(MAX(id), 0) FROM " + table)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static long count(Statement s, String table) throws SQLException {
        try (var rs = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    @AfterEach
    void reset() {
        push.reset();
    }

    /**
     * SC-001: 20 lần kích hoạt Khẩn cấp, mỗi lần 6–10 người nhận; mọi lần đẩy FCM (giả) đến trong ≤ 10 giây tính từ
     * trước lệnh nguồn (sớm hơn mốc tiếp nhận nên chặt hơn FR-017).
     */
    @Test
    void SC_001_twentyUrgentActivations_everyRecipientWithin10Seconds() {
        TestUsers users = new TestUsers(jdbc, time);
        List<TestUser> staff = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        for (int k = 0; k < 10; k++) {
            TestUser u = users.staff(Role.NURSING_STAFF);
            String token = "sc001-" + UUID.randomUUID();
            jdbc.update(
                    "INSERT INTO push_device (account_id, device_id, app, fcm_token, updated_at)"
                            + " VALUES (?, ?, 'STAFF_APP', ?, UTC_TIMESTAMP(6))",
                    u.accountId(),
                    UUID.randomUUID().toString(),
                    token);
            staff.add(u);
            tokens.add(token);
        }

        List<Long> latencies = new ArrayList<>();
        for (int run = 0; run < 20; run++) {
            int size = 6 + run % 5;
            List<TestUser> recipients = staff.subList(0, size);
            List<String> expected = tokens.subList(0, size);
            String key = "sc001-" + UUID.randomUUID();
            push.reset();
            Instant start = Instant.now();
            sourceCommand("009 T044", new SampleSourceEvent(List.of(urgent(key, recipients))));
            await().atMost(Duration.ofSeconds(30))
                    .until(() ->
                            expected.stream().allMatch(t -> !push.pushesTo(t).isEmpty()));
            for (String t : expected) {
                latencies.add(
                        Duration.between(start, push.pushesTo(t).getFirst().at())
                                .toMillis());
            }
        }

        List<Long> sorted = latencies.stream().sorted().toList();
        System.out.printf(
                "[T044] SC-001: %d lần đẩy, p50 %d ms, p95 %d ms, tối đa %d ms%n",
                sorted.size(), sorted.get(sorted.size() / 2), sorted.get(sorted.size() * 95 / 100), sorted.getLast());
        assertThat(sorted).hasSize(160).allSatisfy(ms -> assertThat(ms).isLessThanOrEqualTo(10_000));
    }

    private NotificationRequest urgent(String key, List<TestUser> recipients) {
        return new NotificationRequest(
                "M05",
                "INCIDENT_URGENT",
                SourceGroup.OTHER,
                new SourceRecord("M05.INCIDENT", key),
                key,
                Optional.of(NotificationLevel.URGENT),
                Optional.empty(),
                Optional.empty(),
                recipients.stream()
                        .<RecipientGroup>map(u -> new RecipientGroup.Staff(u.staffId(), "X"))
                        .toList(),
                List.of(new ContentPart(InformationType.GENERAL, "Ngã trong phòng")),
                Optional.of("Có sự cố khẩn cấp"),
                Optional.of("Sự cố"),
                Optional.empty(),
                false,
                time.now(),
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty());
    }

    /**
     * Truy vấn đọc bảng chung trên đường Khẩn cấp và của các job 20 giây: mỗi truy vấn ≤ 1 giây (research B-03: outbox
     * < 1 giây, xác định người nhận ≤ 1 giây) trên dữ liệu một năm.
     */
    @Test
    void T044_queriesOnSharedTables_eachWithinOneSecond() {
        long attempt = eventBase + 2;
        long notification = notificationBase + 1;
        String busyStaff = "STAFF:" + STAFF_BASE;
        Instant now = Instant.now();
        Map<String, Supplier<?>> queries = new LinkedHashMap<>();
        queries.put("DeliveryStore.pending", () -> deliveries.pending(attempt));
        queries.put("DeliveryStore.retryable", () -> deliveries.retryable(3, 100));
        queries.put("InboxStore.page", () -> inbox.page(busyStaff, new InboxFilter(null, null, null, null), null, 20));
        queries.put(
                "InboxStore.page unread",
                () -> inbox.page(busyStaff, new InboxFilter(null, true, null, null), null, 20));
        queries.put("CallReminderStore.due", () -> reminders.due(now.minus(Duration.ofMinutes(5)), 50));
        queries.put(
                "DirectContactStore.unreached", () -> directContacts.unreached(now.minus(Duration.ofDays(1)), 3, 100));
        queries.put("SmsWindowStore.due", () -> smsWindows.due(now));
        queries.put("HeldNotificationStore.due", () -> held.due(now, 100));
        queries.put("LookupStore.bySource", () -> lookup.bySource("M05.INCIDENT", "perf-0"));
        queries.put("LookupStore.channels", () -> lookup.channels(notification));
        // Danh sách lỗi gửi: chỉ đo, không xét 1 giây (lọc FIND_IN_SET trên notification_request.flags quét toàn bảng,
        // có từ trước T043, không đọc bảng chung; báo chủ dự án)
        queries.put("LookupStore.errors", () -> lookup.errors(null));

        Map<String, Long> slowest = new LinkedHashMap<>();
        queries.forEach((name, query) -> {
            query.get();
            long worst = 0;
            for (int k = 0; k < 10; k++) {
                long t = System.nanoTime();
                query.get();
                worst = Math.max(worst, (System.nanoTime() - t) / 1_000_000);
            }
            slowest.put(name, worst);
            System.out.printf("[T044] %-32s tối đa %5d ms (10 lần)%n", name, worst);
        });
        slowest.remove("LookupStore.errors");
        assertThat(slowest).allSatisfy((name, ms) -> assertThat(ms).as(name).isLessThanOrEqualTo(1_000));
    }
}
