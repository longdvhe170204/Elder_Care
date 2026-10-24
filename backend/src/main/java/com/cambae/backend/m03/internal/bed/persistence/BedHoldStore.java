package com.cambae.backend.m03.internal.bed.persistence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** SQL giữ chỗ; trạng thái giường vẫn chỉ được BedStateService ghi. */
@Repository
public class BedHoldStore {
    public record Held(long bedId, long residentId, long waitingId, String reason, Instant until) {}

    public record Active(long allocationId, long bedId) {}

    private final JdbcTemplate jdbc;

    public BedHoldStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Held> expired(Instant now) {
        return jdbc.query(
                "SELECT id,hold_resident_id,hold_waiting_record_id,hold_reason,hold_until FROM bed WHERE state='HELD' AND hold_until<=? AND (hold_reason='WAITING_RECORD' OR hold_expiry_notified_at IS NULL) ORDER BY id",
                (r, i) -> new Held(
                        r.getLong(1),
                        r.getLong(2),
                        r.getLong(3),
                        r.getString(4),
                        r.getObject(5, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                date(now));
    }

    public boolean stillExpired(long bedId, Instant now) {
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed WHERE id=? AND state='HELD' AND hold_until<=? AND (hold_reason='WAITING_RECORD' OR hold_expiry_notified_at IS NULL)",
                        Long.class,
                        bedId,
                        date(now))
                > 0;
    }

    public void markExpiry(long bedId, Instant now) {
        jdbc.update("UPDATE bed SET hold_expiry_notified_at=?,version=version+1 WHERE id=?", date(now), bedId);
    }

    public List<Held> waiting(long waitingId) {
        return jdbc.query(
                "SELECT id,hold_resident_id,hold_waiting_record_id,hold_reason,hold_until FROM bed WHERE state='HELD' AND hold_waiting_record_id=? ORDER BY id",
                (r, i) -> new Held(
                        r.getLong(1),
                        r.getLong(2),
                        r.getLong(3),
                        r.getString(4),
                        r.getObject(5, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                waitingId);
    }

    public boolean overlaps(long bedId, Instant from, Instant until) {
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM v_current_allocation WHERE bed_id=? AND status IN ('ACTIVE','FUTURE') AND start_at<? AND effective_end>?",
                        Long.class,
                        bedId,
                        date(until),
                        date(from))
                > 0;
    }

    public boolean waitingCurrent(long bedId, long waitingId) {
        return !jdbc.queryForList(
                        "SELECT id FROM bed WHERE id=? AND state='HELD' AND hold_reason='WAITING_RECORD' AND hold_waiting_record_id=? FOR UPDATE",
                        Long.class,
                        bedId,
                        waitingId)
                .isEmpty();
    }

    public boolean extensionOverlaps(long id, Instant end) {
        return !jdbc.queryForList(
                        "SELECT other.id FROM bed_allocation locked JOIN v_current_allocation current ON current.id=locked.id JOIN v_current_allocation other ON other.bed_id=current.bed_id AND other.id<>current.id WHERE current.id=? AND other.status IN ('ACTIVE','FUTURE','CLOSED') AND other.start_at<COALESCE(?,'9999-12-31') AND other.effective_end>current.start_at FOR UPDATE OF locked",
                        Long.class,
                        id,
                        date(end))
                .isEmpty();
    }

    public Active active(long residentId) {
        return jdbc
                .query(
                        "SELECT id,bed_id FROM bed_allocation WHERE resident_id=? AND status='ACTIVE'",
                        (r, i) -> new Active(r.getLong(1), r.getLong(2)),
                        residentId)
                .stream()
                .findFirst()
                .orElseThrow(com.cambae.backend.common.error.NotFoundException::new);
    }

    public boolean heldAbsent(long bedId, long residentId) {
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed WHERE id=? AND state='HELD' AND hold_reason='ABSENT_RESIDENT' AND hold_resident_id=?",
                        Long.class,
                        bedId,
                        residentId)
                > 0;
    }

    public void close(long id, Instant at, Long actor) {
        jdbc.update(
                "UPDATE bed_allocation SET status='CLOSED',end_at=?,end_reason='RELEASE_WHEN_ABSENT',closed_by_id=? WHERE id=? AND status='ACTIVE'",
                date(at),
                actor,
                id);
    }

    public void extend(long id, Instant end) {
        jdbc.update("UPDATE bed_allocation SET expected_end=? WHERE id=? AND status='ACTIVE'", date(end), id);
    }

    public static LocalDateTime date(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
