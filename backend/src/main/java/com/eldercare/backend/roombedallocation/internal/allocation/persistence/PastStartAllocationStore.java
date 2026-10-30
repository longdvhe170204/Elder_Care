package com.eldercare.backend.roombedallocation.internal.allocation.persistence;

import com.eldercare.backend.roombedallocation.internal.allocation.domain.PastStartAllocation;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T044: lưu chính mốc đã báo, mốc phân bổ đổi thì được báo lại. */
@Repository
public class PastStartAllocationStore {
    private final JdbcTemplate jdbc;

    public PastStartAllocationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PastStartAllocation> due(Instant now) {
        return jdbc.query(
                "SELECT id,resident_id,bed_id,start_at,floor_id FROM v_current_allocation WHERE status='FUTURE' AND start_at<? AND (notified_planned_start_at IS NULL OR notified_planned_start_at<>start_at) ORDER BY id",
                (rs, i) -> new PastStartAllocation(
                        rs.getLong("id"),
                        rs.getLong("resident_id"),
                        rs.getLong("bed_id"),
                        rs.getTimestamp("start_at").toInstant(),
                        rs.getLong("floor_id")),
                Timestamp.from(now));
    }

    public boolean mark(long id, Instant planned) {
        return jdbc.update(
                        "UPDATE bed_allocation SET notified_planned_start_at=? WHERE id=? AND status='FUTURE' AND (notified_planned_start_at IS NULL OR notified_planned_start_at<>?)",
                        Timestamp.from(planned),
                        id,
                        Timestamp.from(planned))
                > 0;
    }
}
