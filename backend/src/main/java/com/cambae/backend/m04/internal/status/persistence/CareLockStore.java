package com.cambae.backend.m04.internal.status.persistence;

import java.util.Collection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** C-03: khóa tăng dần để các lô chồng nhau không đảo thứ tự khóa. */
@Repository
public class CareLockStore {
    private final JdbcTemplate jdbc;

    public CareLockStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void lock(Collection<Long> residentIds) {
        for (long id : residentIds.stream().distinct().sorted().toList()) {
            // Upsert lấy exclusive lock ngay cả khi dòng đã có; tránh nâng shared lock của INSERT IGNORE.
            jdbc.update(
                    "INSERT INTO care_resident_lock(resident_id) VALUES (?) ON DUPLICATE KEY UPDATE resident_id=resident_id",
                    id);
            jdbc.queryForObject(
                    "SELECT resident_id FROM care_resident_lock WHERE resident_id=? FOR UPDATE", Long.class, id);
        }
    }
}
