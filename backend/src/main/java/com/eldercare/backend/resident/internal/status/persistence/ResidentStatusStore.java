package com.eldercare.backend.resident.internal.status.persistence;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Đọc trạng thái hiện tại của hồ sơ người cao tuổi (DBR-01: đúng một trạng thái). */
@Repository
public class ResidentStatusStore {

    private final JdbcTemplate jdbc;

    ResidentStatusStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ResidentStatus> find(long residentId) {
        return jdbc
                .query(
                        "SELECT status FROM resident WHERE id = ?",
                        (rs, i) -> ResidentStatus.valueOf(rs.getString(1)),
                        residentId)
                .stream()
                .findFirst();
    }

    /** Mã các hồ sơ đang ở trạng thái cho trước. */
    public List<Long> idsByStatus(ResidentStatus status) {
        return jdbc.query("SELECT id FROM resident WHERE status = ?", (rs, i) -> rs.getLong(1), status.name());
    }
}
