package com.cambae.backend.m03.internal.allocation.persistence;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m03.internal.allocation.domain.*;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Ghi vòng đời phân bổ trong transaction lệnh; không ghi trạng thái giường. */
@Repository
public class AllocationCommandStore {
    private final JdbcTemplate jdbc;

    public AllocationCommandStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AllocationRecord get(long id) {
        return get(id, false);
    }

    public AllocationRecord current(long id) {
        return get(id, true);
    }

    private AllocationRecord get(long id, boolean lock) {
        return jdbc
                .query(
                        "SELECT id,resident_id,bed_id,status,start_at,expected_end FROM bed_allocation WHERE id=?"
                                + (lock ? " FOR UPDATE" : ""),
                        (rs, i) -> new AllocationRecord(
                                rs.getLong("id"),
                                rs.getLong("resident_id"),
                                rs.getLong("bed_id"),
                                AllocationStatus.valueOf(rs.getString("status")),
                                rs.getTimestamp("start_at").toInstant(),
                                rs.getTimestamp("expected_end") == null
                                        ? null
                                        : rs.getTimestamp("expected_end").toInstant()),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public List<Long> heldBeds(long residentId) {
        return jdbc.queryForList(
                "SELECT id FROM bed WHERE hold_resident_id=? AND hold_reason='WAITING_RECORD' ORDER BY id",
                Long.class,
                residentId);
    }

    public List<Long> waitingRecords(long residentId) {
        return jdbc.queryForList(
                "SELECT DISTINCT hold_waiting_record_id FROM bed WHERE hold_resident_id=? AND hold_reason='WAITING_RECORD' ORDER BY hold_waiting_record_id",
                Long.class,
                residentId);
    }

    public boolean hasOpen(long residentId) {
        return !jdbc.queryForList(
                        "SELECT id FROM bed_allocation WHERE resident_id=? AND status IN ('ACTIVE','FUTURE') FOR UPDATE",
                        Long.class,
                        residentId)
                .isEmpty();
    }

    public long create(
            long residentId,
            long bedId,
            AllocationStatus status,
            Instant start,
            Instant expectedEnd,
            String reason,
            long actor,
            CheckedCareLevel level) {
        var key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,expected_end,start_reason,opened_by_id,checked_care_level,care_level_source) VALUES(?,?,?,?,?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, residentId);
                    statement.setLong(2, bedId);
                    statement.setString(3, status.name());
                    statement.setTimestamp(4, Timestamp.from(start));
                    statement.setTimestamp(5, expectedEnd == null ? null : Timestamp.from(expectedEnd));
                    statement.setString(6, reason);
                    statement.setLong(7, actor);
                    statement.setString(8, level.level().name());
                    statement.setString(9, level.source().name());
                    return statement;
                },
                key);
        return key.getKey().longValue();
    }

    public void clearWithoutBed(long residentId) {
        jdbc.update("UPDATE resident_position SET without_bed_since=NULL WHERE resident_id=?", residentId);
        jdbc.update(
                "DELETE FROM allocation_reminder WHERE target_type='RESIDENT' AND target_id=? AND flag_type='WITHOUT_BED'",
                residentId);
    }

    public void cancel(long id, String reason) {
        jdbc.update("UPDATE bed_allocation SET status='CANCELLED',cancellation_reason=? WHERE id=?", reason, id);
    }

    public boolean markUnsuitable(long id, boolean unsuitable) {
        return jdbc.update(
                        "UPDATE bed_allocation SET room_unsuitable=? WHERE id=? AND status IN ('ACTIVE','FUTURE') AND room_unsuitable<>?",
                        unsuitable,
                        id,
                        unsuitable)
                > 0;
    }

    public List<AllocationRecord> openCurrent(long residentId, boolean active, boolean future) {
        List<AllocationRecord> rows = new java.util.ArrayList<>();
        if (active) rows.addAll(openByUniqueResident(residentId, "active_resident_id"));
        if (future) rows.addAll(openByUniqueResident(residentId, "future_resident_id"));
        return List.copyOf(rows);
    }

    private List<AllocationRecord> openByUniqueResident(long residentId, String column) {
        return jdbc.query(
                "SELECT id,resident_id,bed_id,status,start_at,expected_end FROM bed_allocation WHERE " + column
                        + "=? FOR SHARE",
                (rs, i) -> new AllocationRecord(
                        rs.getLong("id"),
                        rs.getLong("resident_id"),
                        rs.getLong("bed_id"),
                        AllocationStatus.valueOf(rs.getString("status")),
                        rs.getTimestamp("start_at").toInstant(),
                        rs.getTimestamp("expected_end") == null
                                ? null
                                : rs.getTimestamp("expected_end").toInstant()),
                residentId);
    }

    public void activate(long id, Instant at, CheckedCareLevel level) {
        jdbc.update(
                "UPDATE bed_allocation SET status='ACTIVE',start_at=?,checked_care_level=?,care_level_source=? WHERE id=?",
                Timestamp.from(at),
                level.level().name(),
                level.source().name(),
                id);
    }

    public void close(long id, Instant at, String reason, Long actor) {
        jdbc.update(
                "UPDATE bed_allocation SET status='CLOSED',end_at=?,end_reason=?,closed_by_id=? WHERE id=?",
                Timestamp.from(at),
                reason,
                actor,
                id);
        jdbc.update("DELETE FROM allocation_reminder WHERE target_type='BED_ALLOCATION' AND target_id=?", id);
    }

    public void awaitingDoctor(long id) {
        jdbc.update("UPDATE bed_allocation SET awaiting_doctor_confirmation=TRUE WHERE id=?", id);
    }

    public boolean markWithoutBed(long residentId, Instant at) {
        if (jdbc.update(
                        "UPDATE resident_position SET without_bed_since=? WHERE resident_id=? AND without_bed_since IS NULL",
                        Timestamp.from(at),
                        residentId)
                == 0) return false;
        jdbc.update(
                "INSERT INTO allocation_reminder(target_type,target_id,flag_type,started_at,last_reminded_at,reminder_count) VALUES('RESIDENT',?,'WITHOUT_BED',?,?,1) ON DUPLICATE KEY UPDATE target_id=allocation_reminder.target_id",
                residentId,
                Timestamp.from(at),
                Timestamp.from(at));
        return true;
    }
}
