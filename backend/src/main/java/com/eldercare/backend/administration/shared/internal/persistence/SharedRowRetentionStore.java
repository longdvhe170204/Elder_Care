package com.eldercare.backend.administration.shared.internal.persistence;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Xóa dòng bảng dùng chung bằng kết nối {@code eldercare_retention} (research R-05, R-11, R-15). */
@Component
public class SharedRowRetentionStore {

    private static final String[] BY_ENTITY = {"status_history", "record_event", "simple_record", "reminder_marker"};
    private static final String[] BY_RESIDENT = {"simple_record", "resident_module_state"};

    private final RetentionDatabase database;
    private final JdbcTemplate jdbc;

    public SharedRowRetentionStore(RetentionDatabase database, JdbcTemplate jdbc) {
        this.database = database;
        this.jdbc = jdbc;
    }

    /** Dòng có loại riêng (đính chính, tệp gắn theo loại dòng) mà {@link #deleteForEntity}, {@link #deleteForResidents} xóa. */
    public List<EntityRef> rowsFor(String entityType, long entityId, boolean resident) {
        String sql = "SELECT kind, id FROM record_event WHERE entity_type = ? AND entity_id = ?"
                + " UNION SELECT kind, id FROM simple_record WHERE (entity_type = ? AND entity_id = ?)"
                + (resident ? " OR resident_id = ?" : "");
        Object[] args = resident
                ? new Object[] {entityType, entityId, entityType, entityId, entityId}
                : new Object[] {entityType, entityId, entityType, entityId};
        return jdbc.query(sql, (rs, i) -> new EntityRef(rs.getString(1), rs.getLong(2)), args);
    }

    public void deleteForEntity(String entityType, Collection<Long> entityIds) {
        String in = String.join(", ", Collections.nCopies(entityIds.size(), "?"));
        Object[] args = java.util.stream.Stream.concat(java.util.stream.Stream.of(entityType), entityIds.stream())
                .toArray();
        for (String table : BY_ENTITY) {
            database.jdbc()
                    .update("DELETE FROM " + table + " WHERE entity_type = ? AND entity_id IN (" + in + ")", args);
        }
    }

    public void deleteForResidents(Collection<Long> residentIds) {
        String in = String.join(", ", Collections.nCopies(residentIds.size(), "?"));
        for (String table : BY_RESIDENT) {
            database.jdbc()
                    .update("DELETE FROM " + table + " WHERE resident_id IN (" + in + ")", residentIds.toArray());
        }
    }
}
