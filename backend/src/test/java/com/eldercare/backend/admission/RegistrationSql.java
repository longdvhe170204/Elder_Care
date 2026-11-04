package com.eldercare.backend.admission;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Đọc trực tiếp dữ liệu 018 ở bảng chung của test (data-model 018 mục 0.1, R-15): khai báo, nghĩa vụ, cờ thường trú ở
 * {@code simple_record}, lần nộp ở {@code record_event}, dấu nhắc ở {@code reminder_marker}. Chỉ dùng để kiểm; lệnh ghi đi
 * qua service, store.
 */
final class RegistrationSql {

    static final String REGISTRATION = "M02.RESIDENCE_REGISTRATION";
    static final String OBLIGATION = "M02.REGISTRATION_OBLIGATION";
    static final String SUBMISSION = "M02.REGISTRATION_SUBMISSION";
    static final String RESIDENT_FLAG = "M02.REGISTRATION_RESIDENT";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private RegistrationSql() {}

    static String status(JdbcTemplate jdbc, long registrationId) {
        return jdbc.queryForObject(
                "SELECT status FROM simple_record WHERE kind = ? AND id = ?",
                String.class,
                REGISTRATION,
                registrationId);
    }

    static int registrationCount(JdbcTemplate jdbc, long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM simple_record WHERE kind = ? AND resident_id = ?",
                Integer.class,
                REGISTRATION,
                residentId);
    }

    static int confirmedCount(JdbcTemplate jdbc, long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM simple_record WHERE kind = ? AND resident_id = ? AND status = 'CONFIRMED'",
                Integer.class,
                REGISTRATION,
                residentId);
    }

    /** Nội dung JSON của khai báo. */
    static JsonNode content(JdbcTemplate jdbc, long registrationId) {
        return JSON.readTree(jdbc.queryForObject(
                "SELECT content FROM simple_record WHERE kind = ? AND id = ?",
                String.class,
                REGISTRATION,
                registrationId));
    }

    /** Khai báo mà khai báo {@code registrationId} thay (đặt lúc lập), hoặc {@code null}. */
    static Long replacedRegistrationId(JdbcTemplate jdbc, long registrationId) {
        JsonNode node = content(jdbc, registrationId).get("replacedRegistrationId");
        return node == null || node.isNull() ? null : node.asLong();
    }

    /** Cờ thường trú cùng xã/phường của hồ sơ; {@code false} khi chưa có dòng. */
    static boolean localPermanentResident(JdbcTemplate jdbc, long residentId) {
        List<String> rows = jdbc.queryForList(
                "SELECT content ->> '$.localPermanentResident' FROM simple_record WHERE kind = ? AND resident_id = ?",
                String.class,
                RESIDENT_FLAG,
                residentId);
        return !rows.isEmpty() && Boolean.parseBoolean(rows.getFirst());
    }

    static int flagRows(JdbcTemplate jdbc, long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM simple_record WHERE kind = ? AND resident_id = ?",
                Integer.class,
                RESIDENT_FLAG,
                residentId);
    }

    static int obligationCount(JdbcTemplate jdbc, long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM simple_record WHERE kind = ? AND resident_id = ?",
                Integer.class,
                OBLIGATION,
                residentId);
    }

    static int openObligationCount(JdbcTemplate jdbc, long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM simple_record WHERE kind = ? AND resident_id = ? AND status = 'OPEN'",
                Integer.class,
                OBLIGATION,
                residentId);
    }

    static int submissionCount(JdbcTemplate jdbc, long registrationId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM record_event WHERE kind = ?"
                        + " AND CAST(content ->> '$.registrationId' AS UNSIGNED) = ?",
                Integer.class,
                SUBMISSION,
                registrationId);
    }

    /** Các lần nộp của khai báo, cũ trước, cùng tên cột như bảng cũ ({@code result}, {@code rejection_reason}…). */
    static List<Map<String, Object>> submissions(JdbcTemplate jdbc, long registrationId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        jdbc.query(
                "SELECT id, content, confirmed_at FROM record_event WHERE kind = ?"
                        + " AND CAST(content ->> '$.registrationId' AS UNSIGNED) = ? ORDER BY id",
                rs -> {
                    JsonNode c = JSON.readTree(rs.getString("content"));
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("registration_id", registrationId);
                    row.put("submitted_date", text(c, "submittedDate"));
                    row.put("channel", text(c, "channel"));
                    row.put("receiving_authority", text(c, "receivingAuthority"));
                    row.put("dossier_code", text(c, "dossierCode"));
                    row.put("result", text(c, "result"));
                    row.put("rejection_reason", text(c, "rejectionReason"));
                    row.put("confirmed_at", rs.getObject("confirmed_at"));
                    rows.add(row);
                },
                SUBMISSION,
                registrationId);
        return rows;
    }

    /** Nghĩa vụ của người, mới nhất trước, cùng tên cột như bảng cũ; thời điểm là {@link LocalDateTime} UTC. */
    static List<Map<String, Object>> obligations(JdbcTemplate jdbc, long residentId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        jdbc.query(
                "SELECT id, status, content FROM simple_record WHERE kind = ? AND resident_id = ? ORDER BY id DESC",
                rs -> {
                    JsonNode c = JSON.readTree(rs.getString("content"));
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("resident_id", residentId);
                    row.put("trigger_type", text(c, "trigger"));
                    row.put("trigger_at", utc(c, "triggerAt"));
                    row.put("due_at", utc(c, "dueAt"));
                    row.put("source_id", c.get("sourceId").asLong());
                    row.put(
                            "registration_id",
                            c.hasNonNull("registrationId")
                                    ? c.get("registrationId").asLong()
                                    : null);
                    row.put("submitted", c.get("submitted").asBoolean());
                    row.put("manager_notified_at", utc(c, "managerNotifiedAt"));
                    row.put("cancelled_at", utc(c, "cancelledAt"));
                    rows.add(row);
                },
                OBLIGATION,
                residentId);
        return rows;
    }

    /** Số dấu nhắc của loại {@code type} ({@code CREATE}, {@code OVERDUE}, {@code RENEWAL}, {@code DEREGISTER}) của người. */
    static int reminderCount(JdbcTemplate jdbc, String type, long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM reminder_marker WHERE kind = 'M02.REMINDER' AND entity_type = ? AND entity_id = ?",
                Integer.class,
                "REGISTRATION_" + type,
                residentId);
    }

    /** Đặt trạng thái khai báo bằng SQL (dựng tình huống mà lệnh nghiệp vụ không tới được); nhả khóa duy nhất. */
    static void forceStatus(JdbcTemplate jdbc, long registrationId, String status) {
        jdbc.update(
                "UPDATE simple_record SET status = ?, unique_key = NULL WHERE kind = ? AND id = ?",
                status,
                REGISTRATION,
                registrationId);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static LocalDateTime utc(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? null : LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
    }
}
