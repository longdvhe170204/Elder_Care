package com.eldercare.backend.support;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Lịch sử trạng thái và mức chăm sóc của M01 sau {@code V001_95} (001 T084, data-model 001 mục 0.1): dòng
 * {@code status_history} của đối tượng {@code M01.RESIDENT}, thuộc tính {@code STATUS} và {@code CARE_LEVEL}. Hai bảng
 * dẫn xuất mang tên cột của bảng cũ ({@code resident_status_history}, {@code care_level_history}) để test giữ nguyên
 * kịch bản, chỉ đổi bảng đích.
 */
public final class ResidentHistoryRows {

    /** Thay {@code resident_status_history} trong mệnh đề {@code FROM} (bí danh {@code h}). */
    public static final String STATUS = "(SELECT id, entity_id AS resident_id, value_before AS from_status,"
            + " value AS to_status, command, starts_at AS occurred_at, actor_id AS performed_by, by_system, reason,"
            + " basis_type, basis_id, NULLIF(details ->> '$.conditionWaiver', 'null') AS condition_waiver"
            + " FROM status_history WHERE entity_type = 'M01.RESIDENT' AND attribute = 'STATUS') h";

    /** Thay {@code care_level_history} trong mệnh đề {@code FROM} (bí danh {@code c}). */
    public static final String CARE_LEVEL = "(SELECT id, entity_id AS resident_id, value AS care_level,"
            + " starts_at AS effective_from, basis_type, basis_id, actor_id AS recorded_by"
            + " FROM status_history WHERE entity_type = 'M01.RESIDENT' AND attribute = 'CARE_LEVEL') c";

    /**
     * Thay {@code reassessment_notice} trong mệnh đề {@code FROM} (bí danh {@code n}): dòng {@code simple_record} loại
     * {@code M01.REASSESSMENT_NOTICE}, nguồn ở đối tượng cha, các mốc thời gian đọc từ nội dung JSON (UTC).
     */
    public static final String NOTICES = "(SELECT id, resident_id, content ->> '$.basis' AS basis,"
            + " CASE entity_type WHEN 'M01.ASSESSMENT' THEN 'ASSESSMENT' WHEN 'M05.INCIDENT' THEN 'INCIDENT'"
            + " WHEN 'M01.RESIDENT_STATUS_HISTORY' THEN 'STATUS_HISTORY' WHEN 'M15.CORRECTION' THEN 'CORRECTION' END"
            + " AS source_type, entity_id AS source_id, " + instant("notifiedAt") + " AS notified_at, "
            + instant("deadlineFrom") + " AS deadline_from, " + instant("overdueAlertedAt") + " AS overdue_alerted_at"
            + " FROM simple_record WHERE kind = 'M01.REASSESSMENT_NOTICE') n";

    private static String instant(String field) {
        return "CAST(REPLACE(REPLACE(NULLIF(content ->> '$." + field
                + "', 'null'), 'T', ' '), 'Z', '') AS DATETIME(6))";
    }

    private static final DateTimeFormatter MICROS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS").withZone(ZoneOffset.UTC);

    private ResidentHistoryRows() {}

    /**
     * Thêm thẳng một dòng lịch sử trạng thái (thay {@code INSERT INTO resident_status_history}); {@code id} {@code null}
     * thì cơ sở dữ liệu cấp, {@code at} là giá trị JDBC của thời điểm.
     */
    public static void insertStatus(
            JdbcTemplate jdbc,
            Long id,
            long residentId,
            String from,
            String to,
            String command,
            Object at,
            long performedBy,
            boolean bySystem,
            String reason) {
        jdbc.update(
                "INSERT INTO status_history (id, entity_type, entity_id, attribute, module, kind, value_before, value,"
                        + " starts_at, command, actor_id, by_system, reason) VALUES (?, 'M01.RESIDENT', ?, 'STATUS', 'M01',"
                        + " 'POINT', ?, ?, ?, ?, ?, ?, ?)",
                id,
                residentId,
                from,
                to,
                at,
                command,
                performedBy,
                bySystem,
                reason);
    }

    /** Thêm thẳng một dòng lịch sử mức chăm sóc (thay {@code INSERT INTO care_level_history}). */
    public static void insertCareLevel(
            JdbcTemplate jdbc,
            long residentId,
            String careLevel,
            Instant effectiveFrom,
            String basisType,
            long basisId,
            long recordedBy) {
        jdbc.update(
                "INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id,"
                        + " by_system, basis_type, basis_id, unique_key) VALUES ('M01.RESIDENT', ?, 'CARE_LEVEL', 'M01',"
                        + " 'POINT', ?, ?, ?, FALSE, ?, ?, ?)",
                residentId,
                careLevel,
                java.sql.Timestamp.from(effectiveFrom),
                recordedBy,
                basisType,
                basisId,
                careLevelKey(residentId, effectiveFrom));
    }

    /**
     * Thêm thẳng một lần báo cần đánh giá lại (thay {@code INSERT INTO reassessment_notice}), khóa
     * {@code M01.RAN:<residentId>:<basis>:<sourceType>:<sourceId>} như backend.
     */
    public static void insertNotice(
            JdbcTemplate jdbc, long residentId, String basis, String sourceType, long sourceId, Instant notifiedAt) {
        String entityType = switch (sourceType) {
            case "ASSESSMENT" -> "M01.ASSESSMENT";
            case "INCIDENT" -> "M05.INCIDENT";
            case "STATUS_HISTORY" -> "M01.RESIDENT_STATUS_HISTORY";
            default -> "M15.CORRECTION";
        };
        jdbc.update(
                "INSERT INTO simple_record (kind, module, resident_id, entity_type, entity_id, status, content,"
                        + " unique_key, created_at, created_by) VALUES ('M01.REASSESSMENT_NOTICE', 'M01', ?, ?, ?,"
                        + " 'NOTIFIED', JSON_OBJECT('basis', ?, 'notifiedAt', ?), ?, ?, 0)",
                residentId,
                entityType,
                sourceId,
                basis,
                notifiedAt.toString(),
                "M01.RAN:" + residentId + ":" + basis + ":" + sourceType + ":" + sourceId,
                java.sql.Timestamp.from(notifiedAt));
    }

    /** {@code M01.CLH:<residentId>:<từ>} (thay {@code UNIQUE (resident_id, effective_from)}). */
    public static String careLevelKey(long residentId, Instant effectiveFrom) {
        return "M01.CLH:" + residentId + ":" + MICROS.format(effectiveFrom);
    }
}
