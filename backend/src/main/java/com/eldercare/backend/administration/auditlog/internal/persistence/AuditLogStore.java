package com.eldercare.backend.administration.auditlog.internal.persistence;

import com.eldercare.backend.administration.auditlog.api.AuditLogRecord;
import com.eldercare.backend.administration.auditlog.internal.domain.AuditLogCriteria;
import com.eldercare.backend.administration.auditlog.internal.domain.AuditLogRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Đọc, ghi {@code audit_log} (nhóm 3, chỉ thêm; 000 FR-045 → FR-050, R-06). */
@Component
public class AuditLogStore {

    private static final String INSERT = "INSERT INTO audit_log (occurred_at, device_time, actor_type, actor_id, role,"
            + " action, entity_type, entity_id, resident_id, data_before, data_after, reason, basis, details,"
            + " exempt_from_purge) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String COLUMNS = "SELECT id, occurred_at, device_time, actor_type, actor_id, role, action,"
            + " entity_type, entity_id, resident_id, data_before, data_after, reason, basis, details FROM audit_log";
    private static final String RECORD =
            "SELECT id, occurred_at, actor_type, actor_id, role, entity_id, reason, details" + " FROM audit_log";
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    AuditLogStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Thêm một dòng; giá trị {@code JSON} đã ghi thành chuỗi. */
    public void insert(
            Instant occurredAt,
            Instant deviceTime,
            String actorType,
            Long actorId,
            String role,
            String action,
            String entityType,
            String entityId,
            Long residentId,
            String before,
            String after,
            String reason,
            String basis,
            String details,
            boolean exemptFromPurge) {
        jdbc.update(
                INSERT,
                utc(occurredAt),
                deviceTime == null ? null : utc(deviceTime),
                actorType,
                actorId,
                role,
                action,
                entityType,
                entityId,
                residentId,
                before,
                after,
                reason,
                basis,
                details,
                exemptFromPurge);
    }

    /** Dòng của một hành động trên một loại đối tượng, mới trước, tối đa {@code limit}. */
    public List<AuditLogRecord> recorded(
            String action, String entityType, String entityId, Long actorAccountId, int limit) {
        StringBuilder sql = new StringBuilder(RECORD + " WHERE action = ? AND entity_type = ?");
        List<Object> args = new ArrayList<>(List.of(action, entityType));
        if (entityId != null) {
            sql.append(" AND entity_id = ?");
            args.add(entityId);
        }
        if (actorAccountId != null) {
            sql.append(" AND actor_type = 'USER' AND actor_id = ?");
            args.add(actorAccountId);
        }
        sql.append(" ORDER BY occurred_at DESC, id DESC LIMIT ").append(limit);
        return jdbc.query(sql.toString(), (rs, i) -> record(rs), args.toArray());
    }

    /** Dòng của một loại đối tượng trong {@code [from, to)} có id sau {@code afterId}, theo id. */
    public List<AuditLogRecord> recordedBetween(String entityType, Instant from, Instant to, long afterId, int limit) {
        return jdbc.query(
                RECORD + " WHERE entity_type=? AND occurred_at>=? AND occurred_at<? AND id>? ORDER BY id LIMIT ?",
                (rs, i) -> record(rs),
                entityType,
                utc(from),
                utc(to),
                afterId,
                limit);
    }

    /** Một trang tra cứu theo {@code criteria}, mới trước, tối đa {@code limit} dòng. */
    public List<AuditLogRow> page(AuditLogCriteria c, int limit) {
        List<String> where = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (c.entityTypes() != null && !c.entityTypes().isEmpty()) {
            where.add("entity_type IN ("
                    + String.join(", ", Collections.nCopies(c.entityTypes().size(), "?")) + ")");
            args.addAll(c.entityTypes());
        }
        if (c.entityId() != null) {
            where.add("entity_id = ?");
            args.add(c.entityId());
        }
        if (c.actorId() != null) {
            where.add("actor_id = ?");
            args.add(c.actorId());
        }
        if (c.actions() != null && !c.actions().isEmpty()) {
            where.add("action IN ("
                    + String.join(",", Collections.nCopies(c.actions().size(), "?")) + ")");
            args.addAll(c.actions());
        }
        if (c.from() != null) {
            where.add("occurred_at >= ?");
            args.add(utc(c.from()));
        }
        if (c.to() != null) {
            where.add("occurred_at <= ?");
            args.add(utc(c.to()));
        }
        if (c.selfApproved() != null) {
            // Dấu tự duyệt nằm ở chi tiết (FR-035); dòng không có dấu là không tự duyệt
            where.add("COALESCE(JSON_CONTAINS(details, 'true', '$.selfApproved'), 0) = ?");
            args.add(c.selfApproved() ? 1 : 0);
        }
        if (c.afterOccurredAt() != null) {
            where.add("(occurred_at < ? OR (occurred_at = ? AND id < ?))");
            args.add(c.afterOccurredAt());
            args.add(c.afterOccurredAt());
            args.add(c.afterId());
        }
        String sql = COLUMNS + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where))
                + " ORDER BY occurred_at DESC, id DESC LIMIT " + limit;
        return jdbc.query(sql, (rs, i) -> row(rs), args.toArray());
    }

    private AuditLogRow row(ResultSet rs) throws SQLException {
        return new AuditLogRow(
                rs.getLong("id"),
                rs.getObject("occurred_at", LocalDateTime.class),
                rs.getObject("device_time", LocalDateTime.class),
                rs.getString("actor_type"),
                rs.getObject("actor_id", Long.class),
                rs.getString("role"),
                rs.getString("action"),
                rs.getString("entity_type"),
                rs.getString("entity_id"),
                rs.getObject("resident_id", Long.class),
                readMap(rs.getString("data_before")),
                readMap(rs.getString("data_after")),
                rs.getString("reason"),
                readMap(rs.getString("basis")),
                readMap(rs.getString("details")));
    }

    private AuditLogRecord record(ResultSet rs) throws SQLException {
        return new AuditLogRecord(
                rs.getLong("id"),
                rs.getObject("occurred_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                "USER".equals(rs.getString("actor_type")) ? rs.getLong("actor_id") : null,
                rs.getString("role"),
                rs.getString("entity_id"),
                rs.getString("reason"),
                readMap(rs.getString("details")));
    }

    private Map<String, Object> readMap(String value) {
        return value == null ? null : json.readValue(value, MAP);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
