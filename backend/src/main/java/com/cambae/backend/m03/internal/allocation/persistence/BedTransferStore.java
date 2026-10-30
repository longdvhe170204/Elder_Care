package com.cambae.backend.m03.internal.allocation.persistence;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m03.internal.bed.domain.BedHold;
import com.cambae.backend.m03.internal.bed.domain.HoldReason;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Metadata chuyển thường; trạng thái giường chỉ được ghi qua BedStateService. */
@Repository
public class BedTransferStore {
    private final JdbcTemplate jdbc;

    public BedTransferStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean medicalSafety(long reasonId) {
        return jdbc
                .query(
                        "SELECT medical_safety FROM bed_transfer_reason WHERE id=? AND status='ACTIVE' FOR SHARE",
                        (r, n) -> r.getBoolean(1),
                        reasonId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new InvalidValueException("Cần lý do chuyển giường đang Hiệu lực"));
    }

    public long roomType(long bedId) {
        return jdbc.queryForObject(
                "SELECT r.room_type_id FROM bed b JOIN room r ON r.id=b.room_id WHERE b.id=? FOR SHARE",
                Long.class,
                bedId);
    }

    public boolean awaitingPriceResolution(long allocationId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT (awaiting_request OR awaiting_same_price) FROM bed_allocation WHERE id=? FOR UPDATE",
                Boolean.class,
                allocationId));
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

    public void transferred(long previous, long next, long reasonId, boolean awaitingRequest, Instant at) {
        jdbc.update(
                "UPDATE bed_allocation SET previous_allocation_id=?,transfer_reason_id=?,awaiting_request=? WHERE id=?",
                previous,
                reasonId,
                awaitingRequest,
                next);
        if (awaitingRequest)
            jdbc.update(
                    "INSERT INTO allocation_reminder(target_type,target_id,flag_type,started_at,last_reminded_at,reminder_count) VALUES('BED_ALLOCATION',?,'AWAITING_REQUEST',?,NULL,0)",
                    next,
                    Timestamp.from(at));
    }
}
