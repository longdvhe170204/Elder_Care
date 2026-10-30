package com.eldercare.backend.administration.shared.internal.persistence;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import java.util.Collection;
import java.util.Collections;
import org.springframework.stereotype.Component;

/** Xóa dòng bảng dùng chung bằng kết nối {@code eldercare_retention} (research R-05, R-11, R-15). */
@Component
public class SharedRowRetentionStore {

    private static final String[] BY_ENTITY = {"status_history", "record_event", "simple_record", "reminder_marker"};
    private static final String[] BY_RESIDENT = {"simple_record", "resident_module_state"};

    private final RetentionDatabase database;

    public SharedRowRetentionStore(RetentionDatabase database) {
        this.database = database;
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
