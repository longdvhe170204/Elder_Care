package com.cambae.backend.m03.internal.bed.persistence;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m03.api.dto.BedStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Metadata sẵn sàng, không ghi trạng thái giường. */
@Repository
public class BedReadinessStore {
    private final JdbcTemplate jdbc;

    public BedReadinessStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Overdue(long bedId, long floorId, Instant deadline) {}

    public List<Overdue> due(Instant at) {
        return overdue(null, at);
    }

    public List<Overdue> overdue(Long id, Instant at) {
        return jdbc.query(
                "SELECT b.id,r.floor_id,b.ready_deadline FROM bed b JOIN room r ON r.id=b.room_id WHERE b.state='AWAITING_READY' AND b.ready_reminded_at IS NULL AND b.ready_deadline<?"
                        + (id == null ? "" : " AND b.id=?") + " ORDER BY b.id",
                (r, n) -> new Overdue(
                        r.getLong(1), r.getLong(2), r.getTimestamp(3).toInstant()),
                id == null
                        ? new Object[] {java.sql.Timestamp.from(at)}
                        : new Object[] {java.sql.Timestamp.from(at), id});
    }

    public void mark(long id, Instant at) {
        jdbc.update("UPDATE bed SET ready_reminded_at=? WHERE id=?", java.sql.Timestamp.from(at), id);
    }

    public record Snapshot(long id, long floorId, BedStatus state, Long departedResidentId) {}

    public Snapshot read(long id) {
        return jdbc
                .query(
                        "SELECT b.id,r.floor_id,b.state,a.resident_id FROM bed b JOIN room r ON r.id=b.room_id LEFT JOIN bed_allocation a ON a.id=b.last_closed_allocation_id WHERE b.id=?",
                        (r, n) -> new Snapshot(
                                r.getLong(1),
                                r.getLong(2),
                                BedStatus.valueOf(r.getString(3)),
                                r.getObject(4, Long.class)),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public record Waiting(long requestId, Instant plannedAt) {}

    public List<Waiting> waiting(long bed) {
        return jdbc.query(
                "SELECT basis_id,COALESCE(planned_at,start_at) FROM bed_allocation WHERE bed_id=? AND status='FUTURE' AND waiting_for_bed_ready=TRUE ORDER BY start_at,id FOR UPDATE",
                (r, n) -> {
                    Long request = r.getObject(1, Long.class);
                    if (request == null) throw new IllegalStateException("Phân bổ chờ chuyển thiếu căn cứ yêu cầu");
                    return new Waiting(request, r.getTimestamp(2).toInstant());
                },
                bed);
    }
}
