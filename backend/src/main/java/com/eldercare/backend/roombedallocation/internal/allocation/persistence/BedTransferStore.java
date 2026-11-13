package com.eldercare.backend.roombedallocation.internal.allocation.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.BackfillRequest;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedHold;
import com.eldercare.backend.roombedallocation.internal.bed.domain.HoldReason;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Metadata chuyển thường; trạng thái giường chỉ được ghi qua BedStateService. */
@Repository
public class BedTransferStore {
    private final JdbcTemplate jdbc;
    private final AllocationReminderMarkers reminders;
    private final RecordEventApi events;

    public BedTransferStore(JdbcTemplate jdbc, AllocationReminderMarkers reminders, RecordEventApi events) {
        this.jdbc = jdbc;
        this.reminders = reminders;
        this.events = events;
    }

    public boolean medicalSafety(long reasonId) {
        return jdbc
                .query(
                        "SELECT COALESCE(JSON_VALUE(attributes,'$.medicalSafety')='true',FALSE) FROM catalog_item"
                                + " WHERE id=? AND catalog_type='M03.BED_TRANSFER_REASON' AND status='ACTIVE' FOR SHARE",
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

    /** Chỉ mục yêu cầu nhập bù chờ duyệt (FR-027 (b), CHK051); ghi cùng transaction lập yêu cầu. */
    public void backfillRequested(long requestId, long residentId, long allocationId, long newBedId, Instant at) {
        events.add(
                BackfillRequest.KIND,
                new EntityRef("M03.BED_ALLOCATION", allocationId),
                new NewRecordEvent<>(
                        new BackfillRequest(requestId, residentId, newBedId),
                        null,
                        null,
                        true,
                        at,
                        null,
                        at,
                        null,
                        "M03.BED_BACKFILL_REQUEST:" + requestId));
    }

    public void transferred(long previous, long next, long reasonId, boolean awaitingRequest, Instant at) {
        jdbc.update(
                "UPDATE bed_allocation SET previous_allocation_id=?,transfer_reason_id=?,awaiting_request=? WHERE id=?",
                previous,
                reasonId,
                awaitingRequest,
                next);
        if (awaitingRequest) reminders.start("BED_ALLOCATION", next, "AWAITING_REQUEST", at);
    }
}
