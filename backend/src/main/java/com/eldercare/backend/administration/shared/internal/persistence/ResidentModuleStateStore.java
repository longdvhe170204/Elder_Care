package com.eldercare.backend.administration.shared.internal.persistence;

import static com.eldercare.backend.administration.shared.internal.persistence.Rows.utc;

import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Đọc, ghi {@code resident_module_state} (khóa {@code (resident_id, module)}; data-model 000 mục 8). */
@Component
public class ResidentModuleStateStore {

    private final JdbcTemplate jdbc;

    public ResidentModuleStateStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> find(String module, long residentId, boolean forUpdate) {
        return jdbc
                .queryForList(
                        "SELECT data FROM resident_module_state WHERE resident_id = ? AND module = ?"
                                + (forUpdate ? " FOR UPDATE" : ""),
                        String.class,
                        residentId,
                        module)
                .stream()
                .findFirst();
    }

    public void save(String module, long residentId, String data, Instant at) {
        jdbc.update(
                "INSERT INTO resident_module_state (resident_id, module, data, version, updated_at) VALUES (?, ?, ?, 0, ?)"
                        + " AS new ON DUPLICATE KEY UPDATE data = new.data, version = resident_module_state.version + 1,"
                        + " updated_at = new.updated_at",
                residentId,
                module,
                data,
                utc(at));
    }
}
