package com.cambae.backend.m03.internal.bed.persistence;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m03.api.dto.BedStatus;
import com.cambae.backend.m03.internal.bed.domain.BedHold;
import com.cambae.backend.m03.internal.bed.domain.BedStateChange;
import com.cambae.backend.m03.internal.bed.domain.BedStateSnapshot;
import com.cambae.backend.m03.internal.bed.domain.BedTransition;
import com.cambae.backend.m03.internal.bed.domain.HoldReason;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T008: SQL trạng thái/lịch sử; writeState chỉ được BedStateService gọi (T009). */
@Repository
public class BedStateStore {
    private final JdbcTemplate jdbc;

    public BedStateStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public BedStateSnapshot lock(long id) {
        return jdbc
                .query(
                        "SELECT b.id,b.room_id,r.floor_id,b.state,b.hold_reason,r.status,b.hold_resident_id FROM bed b JOIN room r ON r.id=b.room_id WHERE b.id=? FOR UPDATE OF b",
                        (rs, i) -> new BedStateSnapshot(
                                rs.getLong(1),
                                rs.getLong(2),
                                rs.getLong(3),
                                BedStatus.valueOf(rs.getString(4)),
                                rs.getString(5) == null ? null : HoldReason.valueOf(rs.getString(5)),
                                "ACTIVE"
                                        .equals(jdbc.queryForObject(
                                                "SELECT status FROM room WHERE id=? FOR SHARE",
                                                String.class,
                                                rs.getLong(2))),
                                rs.getObject(7, Long.class)),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public boolean hasHistory(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM bed_state_history WHERE bed_id=?", Long.class, id) > 0;
    }

    public boolean refreshAbsentHold(long id, BedHold hold) {
        return jdbc.update(
                        "UPDATE bed SET hold_until=?,hold_expiry_notified_at=NULL,version=version+1 WHERE id=? AND state='HELD' AND hold_reason='ABSENT_RESIDENT' AND hold_resident_id=?",
                        time(hold.until()),
                        id,
                        hold.residentId())
                == 1;
    }

    public BedStatus maintenanceSource(long id) {
        return jdbc
                .query(
                        "SELECT from_state FROM bed_state_history WHERE bed_id=? AND event='ASSET_MAINTENANCE' ORDER BY id DESC LIMIT 1",
                        (rs, i) -> BedStatus.valueOf(rs.getString(1)),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Thiếu lịch sử vào bảo trì"));
    }

    public String suspensionReason(long id) {
        return jdbc
                .query(
                        "SELECT reason FROM bed_state_history WHERE bed_id=? AND event='SUSPEND' ORDER BY id DESC LIMIT 1",
                        (rs, i) -> rs.getString(1),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Thiếu lý do tạm ngừng"));
    }

    public void writeState(
            long id,
            BedStatus state,
            BedHold hold,
            Instant at,
            Instant deadline,
            Long allocationId,
            String suspensionReason) {
        jdbc.update(
                "UPDATE bed SET state=?,hold_reason=?,hold_resident_id=?,hold_waiting_record_id=?,hold_until=?,hold_expiry_notified_at=NULL,"
                        + "awaiting_ready_since=?,ready_deadline=?,last_closed_allocation_id=?,ready_reminded_at=NULL,suspension_reason=?,version=version+1 WHERE id=?",
                state.name(),
                hold == null ? null : hold.reason().name(),
                hold == null ? null : hold.residentId(),
                hold == null ? null : hold.waitingRecordId(),
                hold == null ? null : time(hold.until()),
                state == BedStatus.AWAITING_READY ? time(at) : null,
                time(deadline),
                state == BedStatus.AWAITING_READY ? allocationId : null,
                suspensionReason,
                id);
    }

    public void appendHistory(BedStateSnapshot bed, BedStateChange change, BedTransition command) {
        jdbc.update(
                "INSERT INTO bed_state_history(bed_id,from_state,to_state,event,occurred_at,planned_at,executed_late,actor_id,reason,basis_type,basis_id,"
                        + "disinfection_required,disinfected,ppe_used,floor_area_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                bed.id(),
                change.before() == null ? null : change.before().name(),
                change.after().name(),
                command.event().name(),
                time(change.occurredAt()),
                time(command.plannedAt()),
                change.executedLate(),
                command.plannedAt() == null ? command.actorId() : null,
                command.reason() == null ? null : command.reason().value(),
                command.basisType(),
                command.basisId(),
                command.disinfectionRequired(),
                command.disinfected(),
                command.ppeUsed(),
                bed.floorId());
    }

    private static LocalDateTime time(Instant at) {
        return at == null ? null : LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}
