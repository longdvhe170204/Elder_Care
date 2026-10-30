package com.eldercare.backend.roombedallocation.internal.isolation.persistence;

import com.eldercare.backend.roombedallocation.internal.bed.domain.BedHold;
import com.eldercare.backend.roombedallocation.internal.bed.domain.HoldReason;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Chỉ metadata phân bổ; mọi trạng thái giường đi qua BedStateService. */
@Repository
public class ControlledTransferStore {
    private final JdbcTemplate jdbc;

    public ControlledTransferStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public BedHold absentHold(long bedId) {
        return jdbc
                .query(
                        "SELECT hold_resident_id,hold_until FROM bed WHERE id=? AND state='HELD' AND hold_reason='ABSENT_RESIDENT' FOR UPDATE",
                        (r, n) -> new BedHold(
                                HoldReason.ABSENT_RESIDENT,
                                r.getLong(1),
                                null,
                                r.getTimestamp(2) == null
                                        ? null
                                        : r.getTimestamp(2).toInstant()),
                        bedId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public boolean pending(long allocation) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT awaiting_doctor_confirmation FROM bed_allocation WHERE id=? FOR UPDATE",
                Boolean.class,
                allocation));
    }

    public void metadata(long previous, long next, long doctor, boolean pending, Instant at) {
        long reason =
                jdbc.queryForObject("SELECT id FROM bed_transfer_reason WHERE code='INFECTION_CONTROL'", Long.class);
        jdbc.update(
                "UPDATE bed_allocation SET previous_allocation_id=?,transfer_reason_id=?,prescribing_doctor_id=?,awaiting_doctor_confirmation=?,doctor_confirmation_requested_at=? WHERE id=?",
                previous,
                reason,
                doctor,
                pending,
                pending ? Timestamp.from(at.truncatedTo(ChronoUnit.MICROS)) : null,
                next);
    }

    public void clearPending(long id) {
        jdbc.update("UPDATE bed_allocation SET awaiting_doctor_confirmation=FALSE WHERE id=?", id);
    }

    public void confirm(long id, long doctor, Instant at, String reason) {
        jdbc.update(
                "UPDATE bed_allocation SET awaiting_doctor_confirmation=FALSE,confirming_doctor_id=?,doctor_confirmed_at=?,doctor_confirmation_reason=? WHERE id=?",
                doctor,
                Timestamp.from(at),
                reason,
                id);
    }
}
