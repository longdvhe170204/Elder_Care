package com.eldercare.backend.administration.shared.internal.persistence;

import static com.eldercare.backend.administration.shared.internal.persistence.Rows.instant;
import static com.eldercare.backend.administration.shared.internal.persistence.Rows.utc;

import com.eldercare.backend.administration.shared.internal.domain.EventRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Đọc, ghi {@code record_event} (nhóm 3; dòng nháp sửa được tới khi xác nhận; data-model 000 mục 8). */
@Component
public class RecordEventStore {

    private static final String COLUMNS = "SELECT id, entity_type, entity_id, content, reply_to_id, actor_id,"
            + " by_system, occurred_at, device_at, confirmed_at, floor_area_id FROM record_event";

    private final JdbcTemplate jdbc;

    public RecordEventStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(String kind, String module, EventRow e, String uniqueKey) {
        jdbc.update(
                "INSERT INTO record_event (entity_type, entity_id, module, kind, content, reply_to_id, actor_id,"
                        + " by_system, occurred_at, device_at, confirmed_at, floor_area_id, unique_key)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                e.entityType(),
                e.entityId(),
                module,
                kind,
                e.content(),
                e.replyToId(),
                e.actorId(),
                e.bySystem(),
                utc(e.occurredAt()),
                utc(e.deviceAt()),
                utc(e.confirmedAt()),
                e.floorAreaId(),
                uniqueKey);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public Optional<EventRow> find(String kind, long id) {
        return jdbc.query(COLUMNS + " WHERE id = ? AND kind = ?", (rs, i) -> row(rs), id, kind).stream()
                .findFirst();
    }

    /** Như {@link #find}, khóa dòng tới hết transaction. */
    public Optional<EventRow> lock(String kind, long id) {
        return jdbc.query(COLUMNS + " WHERE id = ? AND kind = ? FOR UPDATE", (rs, i) -> row(rs), id, kind).stream()
                .findFirst();
    }

    public Optional<EventRow> findByUniqueKey(String kind, String uniqueKey) {
        return jdbc.query(COLUMNS + " WHERE kind = ? AND unique_key = ?", (rs, i) -> row(rs), kind, uniqueKey).stream()
                .findFirst();
    }

    public boolean existsUniqueKey(String uniqueKey) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM record_event WHERE unique_key = ?)", Boolean.class, uniqueKey));
    }

    public List<EventRow> list(String kind, String entityType, long entityId) {
        return jdbc.query(
                COLUMNS + " WHERE kind = ? AND entity_type = ? AND entity_id = ? ORDER BY occurred_at, id",
                (rs, i) -> row(rs),
                kind,
                entityType,
                entityId);
    }

    /** Ghi nội dung, thời điểm xác nhận của dòng nháp. */
    public void updateDraft(long id, String content, Instant confirmedAt) {
        jdbc.update(
                "UPDATE record_event SET content = ?, confirmed_at = ? WHERE id = ? AND confirmed_at IS NULL",
                content,
                utc(confirmedAt),
                id);
    }

    private static EventRow row(ResultSet rs) throws SQLException {
        return new EventRow(
                rs.getLong("id"),
                rs.getString("entity_type"),
                rs.getLong("entity_id"),
                rs.getString("content"),
                rs.getObject("reply_to_id", Long.class),
                rs.getObject("actor_id", Long.class),
                rs.getBoolean("by_system"),
                instant(rs, "occurred_at"),
                instant(rs, "device_at"),
                instant(rs, "confirmed_at"),
                rs.getObject("floor_area_id", Long.class));
    }
}
