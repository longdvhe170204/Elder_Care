package com.eldercare.backend.resident.internal.status.persistence;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Đọc trạng thái hiện tại của hồ sơ người cao tuổi (DBR-01: đúng một trạng thái); ghi mốc đã cảnh báo Tạm vắng quá hạn. */
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

    /** Ghi đã cảnh báo cho mốc trở lại này; {@code false} khi mốc đã được cảnh báo (FR-050). */
    public boolean recordOverdueAlert(long absenceId, Instant expectedReturnAt, long residentId, Instant at) {
        return jdbc.update(
                        "INSERT IGNORE INTO return_overdue_alert (absence_id, expected_return_at, resident_id, alerted_at)"
                                + " VALUES (?, ?, ?, ?)",
                        absenceId,
                        Timestamp.from(expectedReturnAt),
                        residentId,
                        Timestamp.from(at))
                == 1;
    }
}
