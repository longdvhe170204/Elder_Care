package com.eldercare.backend.roombedallocation.internal.bed.persistence;

import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import com.eldercare.backend.roombedallocation.internal.bed.domain.OverdueBedReadiness;
import com.eldercare.backend.roombedallocation.internal.bed.domain.ReadinessBedSnapshot;
import com.eldercare.backend.roombedallocation.internal.bed.domain.WaitingBedTransfer;
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

    public List<OverdueBedReadiness> due(Instant at) {
        return overdue(null, at);
    }

    public List<OverdueBedReadiness> overdue(Long id, Instant at) {
        return jdbc.query(
                "SELECT b.id,r.floor_id,b.ready_deadline FROM bed b JOIN room r ON r.id=b.room_id WHERE b.state='AWAITING_READY' AND b.ready_reminded_at IS NULL AND b.ready_deadline<?"
                        + (id == null ? "" : " AND b.id=?") + " ORDER BY b.id",
                (r, n) -> new OverdueBedReadiness(
                        r.getLong(1), r.getLong(2), r.getTimestamp(3).toInstant()),
                id == null
                        ? new Object[] {java.sql.Timestamp.from(at)}
                        : new Object[] {java.sql.Timestamp.from(at), id});
    }

    public void mark(long id, Instant at) {
        jdbc.update("UPDATE bed SET ready_reminded_at=? WHERE id=?", java.sql.Timestamp.from(at), id);
    }

    public ReadinessBedSnapshot read(long id) {
        return jdbc
                .query(
                        "SELECT b.id,r.floor_id,b.state,a.resident_id FROM bed b JOIN room r ON r.id=b.room_id LEFT JOIN bed_allocation a ON a.id=b.last_closed_allocation_id WHERE b.id=?",
                        (r, n) -> new ReadinessBedSnapshot(
                                r.getLong(1),
                                r.getLong(2),
                                BedStatus.valueOf(r.getString(3)),
                                r.getObject(4, Long.class)),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public List<WaitingBedTransfer> waiting(long bed) {
        return jdbc.query(
                "SELECT basis_id,COALESCE(planned_at,start_at) FROM bed_allocation WHERE bed_id=? AND status='FUTURE' AND waiting_for_bed_ready=TRUE ORDER BY start_at,id FOR UPDATE",
                (r, n) -> {
                    Long request = r.getObject(1, Long.class);
                    if (request == null) throw new IllegalStateException("Phân bổ chờ chuyển thiếu căn cứ yêu cầu");
                    return new WaitingBedTransfer(request, r.getTimestamp(2).toInstant());
                },
                bed);
    }
}
