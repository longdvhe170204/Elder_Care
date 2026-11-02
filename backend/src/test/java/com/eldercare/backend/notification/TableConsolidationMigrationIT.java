package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 009 T043 (research R-15 mục 5, data-model 009 mục 0.1): {@code V009_95} chép đủ dòng của 16 bảng cũ vào cột JSON, cột
 * dẫn xuất và bảng chung, đổi các id tham chiếu chéo (khung gộp, lần gửi tổng hợp, liên hệ trực tiếp) sang id mới rồi bỏ
 * bảng cũ. Chạy trên schema tạm: migration tới {@code 9.05}, ghi một bộ dữ liệu cũ, chạy {@code 9.95}.
 */
class TableConsolidationMigrationIT extends MySqlIntegrationTestBase {

    private static final String SUFFIX = "m009_95";
    private static final List<String> LEGACY_TABLES = List.of(
            "notification_channel",
            "notification_request_duplicate",
            "notification_request_content",
            "notification_request_recipient_group",
            "notification_event",
            "notification_state",
            "delivery_attempt",
            "delivery_result",
            "m13_job_run",
            "sms_batch_window",
            "direct_contact",
            "direct_contact_result",
            "call_reminder_notification",
            "call_reminder_handoff",
            "call_reminder_state",
            "call_result");

    private static JdbcTemplate db;
    private static long request;
    private static long notification;
    private static long inAppAttempt;
    private static long batchedAttempt;
    private static long digestAttempt;
    private static long closedWindow;
    private static long contacted;
    private static long reminder;

    @BeforeAll
    static void migrateLegacyData() {
        Flyway upTo905 = Flyway.configure()
                .configuration(scratchFlyway(SUFFIX).getConfiguration())
                .target("9.05")
                .load();
        upTo905.migrate();
        db = new JdbcTemplate(upTo905.getConfiguration().getDataSource());

        db.update("UPDATE notification_channel SET levels = 'MEDIUM,URGENT', version = 3 WHERE code = 'SMS'");
        db.update("INSERT INTO notification_request (source_module, event_type, source_record_type, source_record_id,"
                + " event_key, level, resident_id, floor_area_id, short_content, received_at, intake_result, flags)"
                + " VALUES ('M05', 'INCIDENT', 'M05.INCIDENT', '7', 'k1', 'URGENT', 11, 3, 'Ngã',"
                + " '2026-01-01 08:00:00', 'VALID', 'MISSING_LEVEL')");
        request = id("notification_request");
        db.update(
                "INSERT INTO notification_request_content (request_id, seq, information_type, content)"
                        + " VALUES (?, 1, 'GENERAL', 'Chung'), (?, 2, 'HEALTH', 'Sức khỏe')",
                request,
                request);
        db.update(
                "INSERT INTO notification_request_recipient_group (request_id, seq, group_code, params, substitution,"
                        + " outcome) VALUES (?, 1, 'DutyGroup', '{\"duty\": \"SHIFT_LEAD\"}', 'DEFAULT', 'HAS_RECIPIENTS')",
                request);
        db.update(
                "INSERT INTO notification_request_duplicate (request_id, received_at) VALUES (?, '2026-01-01 08:05:00')",
                request);
        db.update(
                "INSERT INTO notification (request_id, recipient_type, recipient_id, account_id, basis, visible_parts,"
                        + " level, created_at) VALUES (?, 'FAMILY_MEMBER', 21, 31, '[\"FamilyMember:21\"]', 'GENERAL,HEALTH',"
                        + " 'URGENT', '2026-01-01 08:00:00')",
                request);
        notification = id("notification");
        db.update(
                "INSERT INTO notification_state (notification_id, status, sent_at, viewed_at, recipient_key, level,"
                        + " resident_id) VALUES (?, 'VIEWED', '2026-01-01 08:00:01', '2026-01-01 08:10:00', 'FAMILY_MEMBER:21',"
                        + " 'URGENT', 11)",
                notification);
        db.update(
                "INSERT INTO notification_event (notification_id, type, occurred_at) VALUES"
                        + " (?, 'SEND', '2026-01-01 08:00:01'), (?, 'VIEW', '2026-01-01 08:10:00')",
                notification,
                notification);
        db.update(
                "INSERT INTO notification_event (notification_id, type, scheduled_send_at, actor_id, occurred_at)"
                        + " VALUES (?, 'RESCHEDULE', '2026-01-01 09:00:00', 5, '2026-01-01 08:20:00')",
                notification);
        db.update(
                "INSERT INTO delivery_attempt (notification_id, channel, address, attempt_no, scheduled_at)"
                        + " VALUES (?, 'IN_APP', NULL, 1, '2026-01-01 08:00:01')",
                notification);
        inAppAttempt = id("delivery_attempt");
        db.update(
                "INSERT INTO sms_batch_window (recipient_key, started_at, ends_at, first_attempt_id)"
                        + " VALUES ('FAMILY_MEMBER:21', '2026-01-01 08:00:01', '2026-01-01 08:30:00', ?)",
                inAppAttempt);
        closedWindow = id("sms_batch_window");
        db.update(
                "INSERT INTO delivery_attempt (notification_id, channel, address, attempt_no, scheduled_at,"
                        + " batch_window_id) VALUES (?, 'SMS', '0901', 1, '2026-01-01 08:00:02', ?)",
                notification,
                closedWindow);
        batchedAttempt = id("delivery_attempt");
        db.update(
                "INSERT INTO delivery_attempt (notification_id, channel, address, attempt_no, scheduled_at, content)"
                        + " VALUES (?, 'SMS', '0901', 2, '2026-01-01 08:30:00', 'Tổng hợp')",
                notification);
        digestAttempt = id("delivery_attempt");
        db.update(
                "UPDATE sms_batch_window SET closed_at = '2026-01-01 08:30:00', digest_attempt_id = ? WHERE id = ?",
                digestAttempt,
                closedWindow);
        db.update(
                "INSERT INTO sms_batch_window (recipient_key, started_at, ends_at, first_attempt_id)"
                        + " VALUES ('STAFF:9', '2026-01-01 08:40:00', '2026-01-01 09:10:00', ?)",
                digestAttempt);
        db.update(
                "INSERT INTO delivery_result (attempt_id, result, occurred_at) VALUES (?, 'SENT', '2026-01-01 08:00:01')",
                inAppAttempt);
        db.update(
                "INSERT INTO delivery_result (attempt_id, result, digest_attempt_id, occurred_at)"
                        + " VALUES (?, 'BATCHED', ?, '2026-01-01 08:30:00')",
                batchedAttempt,
                digestAttempt);
        db.update(
                "INSERT INTO delivery_result (attempt_id, result, reason, occurred_at)"
                        + " VALUES (?, 'FAILED', 'TEMPORARY: timeout', '2026-01-01 08:30:01')",
                digestAttempt);
        db.update(
                "INSERT INTO direct_contact (notification_id, family_member_id, full_name, phone_number,"
                        + " short_content, created_at) VALUES (?, 21, 'Bà B', '0901', 'Ngã', '2026-01-01 09:00:00')",
                notification);
        contacted = id("direct_contact");
        db.update(
                "INSERT INTO direct_contact_result (direct_contact_id, contacted_by, occurred_at, result, note,"
                        + " recorded_at) VALUES (?, 41, '2026-01-01 09:30:00', 'CONTACTED', 'Đã báo', '2026-01-01 09:31:00')",
                contacted);
        db.update("UPDATE direct_contact SET status = 'CONTACTED', version = version + 1 WHERE id = ?", contacted);
        db.update("INSERT INTO call_reminder (callee_type, callee_id, resident_id, floor_area_id, creation_reason,"
                + " created_at) VALUES ('FAMILY_MEMBER', 21, 11, 3, 'UNACKNOWLEDGED', '2026-01-01 08:15:00')");
        reminder = id("call_reminder");
        db.update(
                "INSERT INTO call_reminder_state (call_reminder_id, status, due_at, current_staff_id, callee_key)"
                        + " VALUES (?, 'AWAITING_RESULT', '2026-01-01 08:45:00', 9, 'FAMILY_MEMBER:21:resident:11')",
                reminder);
        db.update(
                "INSERT INTO call_reminder_notification (call_reminder_id, notification_id, merged_at)"
                        + " VALUES (?, ?, '2026-01-01 08:15:00')",
                reminder,
                notification);
        db.update(
                "INSERT INTO call_reminder_handoff (call_reminder_id, reminded_staff_id, tier, reason, due_at,"
                        + " occurred_at) VALUES (?, 9, 'SHIFT_LEAD', 'INITIAL', '2026-01-01 08:45:00', '2026-01-01 08:15:00')",
                reminder);
        db.update(
                "INSERT INTO call_result (call_reminder_id, called_by, called_at, result, reason, recorded_at)"
                        + " VALUES (?, 51, '2026-01-01 08:20:00', 'NOT_REACHED', 'NO_ANSWER', '2026-01-01 08:21:00')",
                reminder);
        db.update("INSERT INTO m13_job_run (job, business_date, scheduler_run_id, completed_at)"
                + " VALUES ('M13_DAILY', '2026-01-01', 'run-1', '2026-01-01 00:05:00')");

        Flyway.configure()
                .configuration(upTo905.getConfiguration())
                .target("9.95")
                .load()
                .migrate();
    }

    @AfterAll
    static void dropSchema() {
        dropScratchSchema(SUFFIX);
    }

    @Test
    void legacyTablesAreDropped_fourTablesKept() {
        assertThat(db.queryForList(
                        "SELECT LOWER(TABLE_NAME) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()",
                        String.class))
                .doesNotContainAnyElementsOf(LEGACY_TABLES)
                .contains("notification_request", "notification", "call_reminder", "push_device");
    }

    @Test
    void requestContentAndGroups_moveToJsonColumns() {
        Map<String, Object> r = db.queryForMap(
                "SELECT JSON_LENGTH(contents) AS parts, JSON_LENGTH(recipient_groups) AS groups_count,"
                        + " JSON_CONTAINS(contents, JSON_OBJECT('seq', 2, 'informationType', 'HEALTH',"
                        + " 'content', 'Sức khỏe')) AS health,"
                        + " JSON_CONTAINS(recipient_groups, JSON_OBJECT('seq', 1, 'groupCode', 'DutyGroup',"
                        + " 'substitution', 'DEFAULT', 'outcome', 'HAS_RECIPIENTS')) AS duty,"
                        + " recipient_groups->>'$[0].params.duty' AS duty_param"
                        + " FROM notification_request WHERE id = ?",
                request);
        assertThat(r)
                .containsEntry("parts", 2L)
                .containsEntry("groups_count", 1L)
                .containsEntry("health", 1L)
                .containsEntry("duty", 1L)
                .containsEntry("duty_param", "SHIFT_LEAD");
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind = 'M13.REQUEST_DUPLICATE'"
                                + " AND entity_type = 'M13.NOTIFICATION_REQUEST' AND entity_id = ?",
                        Long.class,
                        request))
                .isEqualTo(1);
    }

    @Test
    void notificationState_movesIntoNotification() {
        assertThat(db.queryForMap(
                        "SELECT status, recipient_key, resident_id, hidden, sent_at IS NOT NULL AS sent,"
                                + " viewed_at IS NOT NULL AS viewed FROM notification WHERE id = ?",
                        notification))
                .containsEntry("status", "VIEWED")
                .containsEntry("recipient_key", "FAMILY_MEMBER:21")
                .containsEntry("resident_id", 11L)
                .containsEntry("hidden", false)
                .containsEntry("sent", 1L)
                .containsEntry("viewed", 1L);
        List<Map<String, Object>> events = db.queryForList(
                "SELECT content->>'$.type' AS type, unique_key, actor_id, by_system FROM record_event"
                        + " WHERE kind = 'M13.NOTIFICATION_EVENT' AND entity_type = 'M13.NOTIFICATION' AND entity_id = ?"
                        + " ORDER BY occurred_at",
                notification);
        assertThat(events).extracting(e -> e.get("type")).containsExactly("SEND", "VIEW", "RESCHEDULE");
        assertThat(events.get(0)).containsEntry("unique_key", "M13.NE:" + notification + ":SEND");
        assertThat(events.get(2))
                .containsEntry("unique_key", null)
                .containsEntry("actor_id", 5L)
                .containsEntry("by_system", false);
    }

    @Test
    void attemptsResultsAndWindows_keepCrossReferences() {
        long newInApp = attempt(inAppAttempt);
        long newBatched = attempt(batchedAttempt);
        long newDigest = attempt(digestAttempt);
        long newWindow = db.queryForObject(
                "SELECT id FROM simple_record WHERE kind = 'M13.SMS_BATCH_WINDOW'"
                        + " AND JSON_VALUE(content, '$.legacy.id' RETURNING SIGNED) = ?",
                Long.class,
                closedWindow);
        assertThat(db.queryForMap(
                        "SELECT content->>'$.channel' AS channel, JSON_VALUE(content, '$.attemptNo' RETURNING SIGNED)"
                                + " AS attempt_no, JSON_VALUE(content, '$.batchWindowId' RETURNING SIGNED) AS window_id,"
                                + " m13_batch_window_id, confirmed_at IS NOT NULL AS confirmed, unique_key"
                                + " FROM record_event WHERE id = ?",
                        newBatched))
                .containsEntry("channel", "SMS")
                .containsEntry("attempt_no", 1L)
                .containsEntry("window_id", newWindow)
                .containsEntry("m13_batch_window_id", newWindow)
                .containsEntry("confirmed", 1L)
                .containsEntry("unique_key", "M13.DA:" + notification + ":SMS:1");
        assertThat(db.queryForMap(
                        "SELECT status, unique_key, JSON_VALUE(content, '$.firstAttemptId' RETURNING SIGNED) AS first_id,"
                                + " JSON_VALUE(content, '$.digestAttemptId' RETURNING SIGNED) AS digest_id"
                                + " FROM simple_record WHERE id = ?",
                        newWindow))
                .containsEntry("status", "CLOSED")
                .containsEntry("unique_key", null)
                .containsEntry("first_id", newInApp)
                .containsEntry("digest_id", newDigest);
        assertThat(db.queryForMap("SELECT status, unique_key FROM simple_record WHERE kind = 'M13.SMS_BATCH_WINDOW'"
                        + " AND content->>'$.recipientKey' = 'STAFF:9'"))
                .containsEntry("status", "OPEN")
                .containsEntry("unique_key", "M13.SBW:STAFF:9");
        assertThat(db.queryForMap(
                        "SELECT content->>'$.result' AS result, JSON_VALUE(content, '$.digestAttemptId' RETURNING SIGNED)"
                                + " AS digest_id, unique_key FROM record_event WHERE kind = 'M13.DELIVERY_RESULT'"
                                + " AND entity_type = 'M13.DELIVERY_ATTEMPT' AND entity_id = ?",
                        newBatched))
                .containsEntry("result", "BATCHED")
                .containsEntry("digest_id", newDigest)
                .containsEntry("unique_key", "M13.DR:" + newBatched);
        assertThat(db.queryForObject(
                        "SELECT m13_result FROM record_event WHERE kind = 'M13.DELIVERY_RESULT' AND entity_id = ?",
                        String.class,
                        newDigest))
                .isEqualTo("FAILED");
    }

    @Test
    void directContactAndCallReminder_moveToSharedTables() {
        Map<String, Object> dc =
                db.queryForMap("SELECT id, status, unique_key, locked_at IS NOT NULL AS locked, entity_type, entity_id,"
                        + " content->>'$.fullName' AS full_name FROM simple_record WHERE kind = 'M13.DIRECT_CONTACT'");
        assertThat(dc)
                .containsEntry("status", "CONTACTED")
                .containsEntry("unique_key", "M13.DC:" + notification)
                .containsEntry("locked", 1L)
                .containsEntry("entity_type", "M13.NOTIFICATION")
                .containsEntry("entity_id", notification)
                .containsEntry("full_name", "Bà B");
        assertThat(db.queryForMap(
                        "SELECT content->>'$.result' AS result, actor_id, unique_key FROM record_event"
                                + " WHERE kind = 'M13.DIRECT_CONTACT_RESULT' AND entity_type = 'M13.DIRECT_CONTACT'"
                                + " AND entity_id = ?",
                        dc.get("id")))
                .containsEntry("result", "CONTACTED")
                .containsEntry("actor_id", 41L)
                .containsEntry("unique_key", "M13.DCR:" + dc.get("id"));

        assertThat(db.queryForMap(
                        "SELECT status, current_staff_id, callee_key, pending_key FROM call_reminder WHERE id = ?",
                        reminder))
                .containsEntry("status", "AWAITING_RESULT")
                .containsEntry("current_staff_id", 9L)
                .containsEntry("pending_key", "FAMILY_MEMBER:21:resident:11");
        assertThat(db.queryForList(
                        "SELECT kind FROM record_event WHERE entity_type = 'M13.CALL_REMINDER' AND entity_id = ?"
                                + " ORDER BY kind",
                        String.class,
                        reminder))
                .containsExactly("M13.CALL_REMINDER_HANDOFF", "M13.CALL_REMINDER_NOTIFICATION", "M13.CALL_RESULT");
        assertThat(db.queryForObject(
                        "SELECT unique_key FROM record_event WHERE kind = 'M13.CALL_REMINDER_NOTIFICATION'",
                        String.class))
                .isEqualTo("M13.CRN:" + notification);
        assertThat(
                        db.queryForMap(
                                "SELECT content->>'$.reason' AS reason, actor_id FROM record_event WHERE kind = 'M13.CALL_RESULT'"))
                .containsEntry("reason", "NO_ANSWER")
                .containsEntry("actor_id", 51L);
    }

    @Test
    void channelsAndJobRuns_moveToCatalogAndMarkers() {
        assertThat(db.queryForMap("SELECT name, version, attributes->'$.enabled' = FALSE AS disabled,"
                        + " JSON_CONTAINS(attributes->'$.levels', '[\"MEDIUM\", \"URGENT\"]') AS levels"
                        + " FROM catalog_item WHERE catalog_type = 'M13.NOTIFICATION_CHANNEL' AND code = 'SMS'"))
                .containsEntry("name", "Tin nhắn")
                .containsEntry("version", 3)
                .containsEntry("disabled", 1L)
                .containsEntry("levels", 1L);
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type = 'M13.NOTIFICATION_CHANNEL'",
                        Long.class))
                .isEqualTo(3);
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind = 'M13.JOB_DONE' AND entity_type = 'M13_DAILY'"
                                + " AND marker = '2026-01-01'",
                        Long.class))
                .isEqualTo(1);
    }

    private static long attempt(long legacyId) {
        return db.queryForObject(
                "SELECT id FROM record_event WHERE kind = 'M13.DELIVERY_ATTEMPT'"
                        + " AND JSON_VALUE(content, '$.legacy.id' RETURNING SIGNED) = ?",
                Long.class,
                legacyId);
    }

    private static long id(String table) {
        return db.queryForObject("SELECT MAX(id) FROM " + table, Long.class);
    }
}
