package com.eldercare.backend.administration.shared.internal.persistence;

import static com.eldercare.backend.administration.shared.internal.persistence.Rows.instant;
import static com.eldercare.backend.administration.shared.internal.persistence.Rows.utc;

import com.eldercare.backend.administration.shared.internal.domain.SimpleRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Đọc, ghi {@code simple_record} (nhóm 2, khóa theo dòng; data-model 000 mục 8). */
@Component
public class SimpleRecordStore {

    private static final String COLUMNS = "SELECT id, resident_id, entity_type, entity_id, status, content,"
            + " effective_from, effective_to, unique_key, locked_at, created_at, created_by, version FROM simple_record";

    private final JdbcTemplate jdbc;

    public SimpleRecordStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(String kind, String module, SimpleRow r) {
        jdbc.update(
                "INSERT INTO simple_record (kind, module, resident_id, entity_type, entity_id, status, content,"
                        + " effective_from, effective_to, unique_key, locked_at, created_at, created_by, version)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                kind,
                module,
                r.residentId(),
                r.entityType(),
                r.entityId(),
                r.status(),
                r.content(),
                r.effectiveFrom(),
                r.effectiveTo(),
                r.uniqueKey(),
                utc(r.lockedAt()),
                utc(r.createdAt()),
                r.createdBy());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public Optional<SimpleRow> find(String kind, long id) {
        return jdbc.query(COLUMNS + " WHERE id = ? AND kind = ?", (rs, i) -> row(rs), id, kind).stream()
                .findFirst();
    }

    public Optional<SimpleRow> lock(String kind, long id) {
        return jdbc.query(COLUMNS + " WHERE id = ? AND kind = ? FOR UPDATE", (rs, i) -> row(rs), id, kind).stream()
                .findFirst();
    }

    public Optional<SimpleRow> findByUniqueKey(String kind, String uniqueKey) {
        return jdbc.query(COLUMNS + " WHERE kind = ? AND unique_key = ?", (rs, i) -> row(rs), kind, uniqueKey).stream()
                .findFirst();
    }

    /** Dòng bất kỳ loại đang giữ {@code uniqueKey}, khác {@code exceptId}. */
    public boolean existsUniqueKey(String uniqueKey, long exceptId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM simple_record WHERE unique_key = ? AND id <> ?)",
                Boolean.class,
                uniqueKey,
                exceptId));
    }

    public List<SimpleRow> listByResident(String kind, long residentId, Collection<String> statuses) {
        List<Object> args = new ArrayList<>(List.of(kind, residentId));
        String filter = "";
        if (!statuses.isEmpty()) {
            filter = " AND status IN ("
                    + String.join(", ", statuses.stream().map(s -> "?").toList()) + ")";
            args.addAll(statuses);
        }
        return jdbc.query(
                COLUMNS + " WHERE kind = ? AND resident_id = ?" + filter + " ORDER BY id",
                (rs, i) -> row(rs),
                args.toArray());
    }

    public List<SimpleRow> listByStatus(String kind, Collection<String> statuses) {
        List<Object> args = new ArrayList<>(List.of(kind));
        args.addAll(statuses);
        return jdbc.query(
                COLUMNS + " WHERE kind = ? AND status IN ("
                        + String.join(", ", statuses.stream().map(s -> "?").toList()) + ") ORDER BY id",
                (rs, i) -> row(rs),
                args.toArray());
    }

    public List<SimpleRow> listByParent(String kind, String entityType, long entityId) {
        return jdbc.query(
                COLUMNS + " WHERE kind = ? AND entity_type = ? AND entity_id = ? ORDER BY id",
                (rs, i) -> row(rs),
                kind,
                entityType,
                entityId);
    }

    /** Ghi thay đổi khi {@code version} còn đúng và dòng chưa khóa; trả {@code false} khi không ghi được. */
    public boolean update(
            long id,
            int version,
            String status,
            String content,
            LocalDate effectiveTo,
            String uniqueKey,
            Instant lockedAt) {
        return jdbc.update(
                        "UPDATE simple_record SET status = ?, content = ?, effective_to = ?, unique_key = ?,"
                                + " locked_at = ?, version = version + 1 WHERE id = ? AND version = ?"
                                + " AND locked_at IS NULL",
                        status,
                        content,
                        effectiveTo,
                        uniqueKey,
                        utc(lockedAt),
                        id,
                        version)
                == 1;
    }

    private static SimpleRow row(ResultSet rs) throws SQLException {
        return new SimpleRow(
                rs.getLong("id"),
                rs.getObject("resident_id", Long.class),
                rs.getString("entity_type"),
                rs.getObject("entity_id", Long.class),
                rs.getString("status"),
                rs.getString("content"),
                rs.getObject("effective_from", LocalDate.class),
                rs.getObject("effective_to", LocalDate.class),
                rs.getString("unique_key"),
                instant(rs, "locked_at"),
                instant(rs, "created_at"),
                rs.getLong("created_by"),
                rs.getInt("version"));
    }
}
