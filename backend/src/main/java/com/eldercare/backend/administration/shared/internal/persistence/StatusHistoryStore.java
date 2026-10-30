package com.eldercare.backend.administration.shared.internal.persistence;

import static com.eldercare.backend.administration.shared.internal.persistence.Rows.instant;
import static com.eldercare.backend.administration.shared.internal.persistence.Rows.utc;

import com.eldercare.backend.administration.shared.api.dto.HistoryKind;
import com.eldercare.backend.administration.shared.internal.domain.HistoryRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Đọc, ghi {@code status_history} (nhóm 3, chỉ thêm; data-model 000 mục 8). */
@Component
public class StatusHistoryStore {

    private static final String COLUMNS = "SELECT id, kind, value_before, value, value_id, starts_at, ends_at, command,"
            + " actor_id, by_system, reason, basis_type, basis_id, details, floor_area_id FROM status_history";
    private static final String OF_TRACK = " WHERE entity_type = ? AND entity_id = ? AND attribute = ?";

    private final JdbcTemplate jdbc;

    public StatusHistoryStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(
            String entityType, long entityId, String attribute, String module, HistoryRecord r, String uniqueKey) {
        jdbc.update(
                "INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value_before, value,"
                        + " value_id, starts_at, command, actor_id, by_system, reason, basis_type, basis_id, details,"
                        + " floor_area_id, unique_key) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                entityType,
                entityId,
                attribute,
                module,
                r.kind().name(),
                r.valueBefore(),
                r.value(),
                r.valueId(),
                utc(r.startsAt()),
                r.command(),
                r.actorId(),
                r.bySystem(),
                r.reason(),
                r.basisType(),
                r.basisId(),
                r.details(),
                r.floorAreaId(),
                uniqueKey);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public boolean existsUniqueKey(String uniqueKey) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM status_history WHERE unique_key = ?)", Boolean.class, uniqueKey));
    }

    /** Khoảng đang mở, khóa dòng tới hết transaction. */
    public Optional<HistoryRecord> lockOpen(String entityType, long entityId, String attribute) {
        return jdbc
                .query(
                        COLUMNS + OF_TRACK + " AND kind = 'PERIOD' AND ends_at IS NULL FOR UPDATE",
                        (rs, i) -> row(rs),
                        entityType,
                        entityId,
                        attribute)
                .stream()
                .findFirst();
    }

    public void close(long id, Instant endsAt) {
        jdbc.update("UPDATE status_history SET ends_at = ? WHERE id = ?", utc(endsAt), id);
    }

    public Optional<HistoryRecord> find(long id) {
        return jdbc.query(COLUMNS + " WHERE id = ?", (rs, i) -> row(rs), id).stream()
                .findFirst();
    }

    public List<HistoryRecord> list(String entityType, long entityId, String attribute) {
        return jdbc.query(
                COLUMNS + OF_TRACK + " ORDER BY starts_at, id", (rs, i) -> row(rs), entityType, entityId, attribute);
    }

    /** Dòng bắt đầu muộn nhất không sau {@code at}; khoảng thì phải chưa kết thúc tại {@code at}. */
    public Optional<HistoryRecord> at(String entityType, long entityId, String attribute, Instant at) {
        return jdbc
                .query(
                        COLUMNS + OF_TRACK
                                + " AND starts_at <= ? AND (kind = 'POINT' OR ends_at IS NULL OR ends_at > ?)"
                                + " ORDER BY starts_at DESC, id DESC LIMIT 1",
                        (rs, i) -> row(rs),
                        entityType,
                        entityId,
                        attribute,
                        utc(at),
                        utc(at))
                .stream()
                .findFirst();
    }

    private static HistoryRecord row(ResultSet rs) throws SQLException {
        return new HistoryRecord(
                rs.getLong("id"),
                HistoryKind.valueOf(rs.getString("kind")),
                rs.getString("value_before"),
                rs.getString("value"),
                rs.getObject("value_id", Long.class),
                instant(rs, "starts_at"),
                instant(rs, "ends_at"),
                rs.getString("command"),
                rs.getObject("actor_id", Long.class),
                rs.getBoolean("by_system"),
                rs.getString("reason"),
                rs.getString("basis_type"),
                rs.getObject("basis_id", Long.class),
                rs.getString("details"),
                rs.getObject("floor_area_id", Long.class));
    }
}
