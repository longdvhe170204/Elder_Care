package com.eldercare.backend.roombedallocation.internal.allocation.persistence;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Metadata T033; khóa nghiệp vụ và trạng thái giường do service điều phối. */
@Repository
public class ScheduledTransferStore {
    private final JdbcTemplate jdbc;

    public ScheduledTransferStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ScheduledAllocation> find(long requestId, boolean current) {
        return jdbc
                .query(
                        "SELECT id,resident_id,bed_id,status,start_at,expected_end,previous_allocation_id,waiting_for_bed_ready,planned_start_at FROM bed_allocation WHERE basis_type='REQUEST' AND basis_id=? AND planned_start_at IS NOT NULL"
                                + (current ? " FOR SHARE" : ""),
                        (r, n) -> new ScheduledAllocation(
                                new AllocationRecord(
                                        r.getLong(1),
                                        r.getLong(2),
                                        r.getLong(3),
                                        AllocationStatus.valueOf(r.getString(4)),
                                        r.getTimestamp(5).toInstant(),
                                        r.getTimestamp(6) == null
                                                ? null
                                                : r.getTimestamp(6).toInstant()),
                                requestId,
                                r.getLong(7),
                                r.getBoolean(8),
                                r.getTimestamp(9).toInstant()),
                        requestId)
                .stream()
                .findFirst();
    }

    public Optional<AllocationRecord> active(long residentId) {
        return jdbc
                .query(
                        "SELECT id,resident_id,bed_id,status,start_at,expected_end FROM bed_allocation WHERE active_resident_id=? FOR SHARE",
                        (r, n) -> new AllocationRecord(
                                r.getLong(1),
                                r.getLong(2),
                                r.getLong(3),
                                AllocationStatus.valueOf(r.getString(4)),
                                r.getTimestamp(5).toInstant(),
                                r.getTimestamp(6) == null
                                        ? null
                                        : r.getTimestamp(6).toInstant()),
                        residentId)
                .stream()
                .findFirst();
    }

    public boolean hasFuture(long residentId) {
        return !jdbc.queryForList(
                        "SELECT id FROM bed_allocation WHERE future_resident_id=? FOR SHARE", Long.class, residentId)
                .isEmpty();
    }

    public void reserved(long id, long requestId, long previous, Instant start) {
        jdbc.update(
                "UPDATE bed_allocation SET basis_type='REQUEST',basis_id=?,previous_allocation_id=?,planned_start_at=?,planned_at=? WHERE id=?",
                requestId,
                previous,
                Timestamp.from(start),
                Timestamp.from(start),
                id);
    }

    public boolean waitForBed(long id) {
        return jdbc.update(
                        "UPDATE bed_allocation SET waiting_for_bed_ready=TRUE WHERE id=? AND waiting_for_bed_ready=FALSE",
                        id)
                > 0;
    }

    public void activated(
            long id,
            long previous,
            Instant actualAt,
            Instant end,
            CheckedCareLevel level,
            Instant planned,
            boolean late,
            Long actor) {
        // Đổi cả khoảng trong một câu lệnh để trigger chỉ kiểm khoảng thực tế cuối cùng.
        jdbc.update(
                "UPDATE bed_allocation SET status='ACTIVE',start_at=?,expected_end=?,checked_care_level=?,care_level_source=?,waiting_for_bed_ready=FALSE,previous_allocation_id=?,planned_at=?,executed_late=?,opened_by_id=? WHERE id=?",
                Timestamp.from(actualAt),
                end == null ? null : Timestamp.from(end),
                level.level().name(),
                level.source().name(),
                previous,
                Timestamp.from(planned),
                late,
                actor,
                id);
    }

    public void stopWaiting(long id) {
        jdbc.update("UPDATE bed_allocation SET waiting_for_bed_ready=FALSE WHERE id=?", id);
    }

    public void closedMetadata(long id, Instant planned, boolean late) {
        // Ghi trước khi đóng vì trigger cấm sửa phân bổ cuối.
        jdbc.update(
                "UPDATE bed_allocation SET planned_at=?,executed_late=? WHERE id=?", Timestamp.from(planned), late, id);
    }

    public boolean linkUrgentRequest(long id, long requestId) {
        var row = jdbc.queryForMap(
                "SELECT basis_type,basis_id,awaiting_request,awaiting_same_price FROM bed_allocation WHERE id=? FOR UPDATE",
                id);
        if ("REQUEST".equals(row.get("basis_type")) && Long.valueOf(requestId).equals(row.get("basis_id")))
            return false;
        if (!Boolean.TRUE.equals(row.get("awaiting_request")) && !Boolean.TRUE.equals(row.get("awaiting_same_price")))
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "URGENT_TRANSFER_REQUEST_REQUIRED",
                    "Phân bổ không chờ yêu cầu sau chuyển gấp");
        jdbc.update(
                "UPDATE bed_allocation SET basis_type='REQUEST',basis_id=?,awaiting_request=FALSE WHERE id=?",
                requestId,
                id);
        jdbc.update(
                "DELETE FROM allocation_reminder WHERE target_type='BED_ALLOCATION' AND target_id=? AND flag_type='AWAITING_REQUEST'",
                id);
        return true;
    }

    public boolean decideUrgentRequest(long id, boolean approved, Instant at) {
        var row = jdbc.queryForMap(
                "SELECT basis_type,awaiting_request,awaiting_same_price FROM bed_allocation WHERE id=? FOR UPDATE", id);
        if (!"REQUEST".equals(row.get("basis_type")) || Boolean.TRUE.equals(row.get("awaiting_request")))
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "URGENT_TRANSFER_REQUEST_REQUIRED",
                    "Cần liên kết yêu cầu trước khi ghi kết quả");
        if (Boolean.TRUE.equals(row.get("awaiting_same_price")) == !approved) return false;
        jdbc.update("UPDATE bed_allocation SET awaiting_same_price=? WHERE id=?", !approved, id);
        clearPriceMarkers(id);
        if (!approved)
            jdbc.update(
                    "INSERT INTO allocation_reminder(target_type,target_id,flag_type,started_at,last_reminded_at,reminder_count) VALUES('BED_ALLOCATION',?,'AWAITING_SAME_PRICE',?,NULL,0)",
                    id,
                    Timestamp.from(at));
        return true;
    }

    public void clearPriceMarkers(long id) {
        jdbc.update(
                "DELETE FROM allocation_reminder WHERE target_type='BED_ALLOCATION' AND target_id=? AND flag_type IN ('AWAITING_REQUEST','AWAITING_SAME_PRICE')",
                id);
    }
}
