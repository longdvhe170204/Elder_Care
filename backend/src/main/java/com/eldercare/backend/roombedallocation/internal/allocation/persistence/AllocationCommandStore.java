package com.eldercare.backend.roombedallocation.internal.allocation.persistence;

import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.*;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Ghi vòng đời phân bổ trong transaction lệnh; không ghi trạng thái giường. */
@Repository
public class AllocationCommandStore {
    private final JdbcTemplate jdbc;
    private final com.eldercare.backend.common.time.BusinessTime time;
    private final AllocationReminderMarkers reminders;
    private final ResidentPositionStore positions;

    public AllocationCommandStore(
            JdbcTemplate jdbc,
            com.eldercare.backend.common.time.BusinessTime time,
            AllocationReminderMarkers reminders,
            ResidentPositionStore positions) {
        this.jdbc = jdbc;
        this.time = time;
        this.reminders = reminders;
        this.positions = positions;
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

    // ---- Chuyển theo lịch, kết quả yêu cầu sau chuyển gấp (003 T033 lượt 2; FR-029, FR-030) ---------------------

    /** Phân bổ tương lai đặt trước cho yêu cầu thay đổi lưu trú; căn cứ là yêu cầu, trỏ về phân bổ đang hiệu lực. */
    public long createScheduled(
            long residentId,
            long bedId,
            Instant start,
            Instant expectedEnd,
            long requestId,
            long previousId,
            Long actor,
            CheckedCareLevel level) {
        var key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,expected_end,start_reason,"
                                    + "opened_by_id,checked_care_level,care_level_source,basis_type,basis_id,"
                                    + "previous_allocation_id) VALUES(?,?,'FUTURE',?,?,?,?,?,?,'APPROVAL_REQUEST',?,?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, residentId);
                    statement.setLong(2, bedId);
                    statement.setTimestamp(3, Timestamp.from(start));
                    statement.setTimestamp(4, expectedEnd == null ? null : Timestamp.from(expectedEnd));
                    statement.setString(5, "Chuyển giường theo lịch");
                    statement.setObject(6, actor);
                    statement.setString(7, level.level().name());
                    statement.setString(8, level.source().name());
                    statement.setLong(9, requestId);
                    statement.setLong(10, previousId);
                    return statement;
                },
                key);
        return key.getKey().longValue();
    }

    /** Phân bổ đặt trước gần nhất của yêu cầu, khóa dòng. */
    public java.util.Optional<AllocationRecord> scheduledFor(long requestId) {
        return jdbc
                .query(
                        "SELECT id,resident_id,bed_id,status,start_at,expected_end FROM bed_allocation"
                                + " WHERE basis_type='APPROVAL_REQUEST' AND basis_id=? ORDER BY id DESC LIMIT 1 FOR UPDATE",
                        (rs, i) -> new AllocationRecord(
                                rs.getLong("id"),
                                rs.getLong("resident_id"),
                                rs.getLong("bed_id"),
                                AllocationStatus.valueOf(rs.getString("status")),
                                rs.getTimestamp("start_at").toInstant(),
                                rs.getTimestamp("expected_end") == null
                                        ? null
                                        : rs.getTimestamp("expected_end").toInstant()),
                        requestId)
                .stream()
                .findFirst();
    }

    public long previousOf(long id) {
        Long previous =
                jdbc.queryForObject("SELECT previous_allocation_id FROM bed_allocation WHERE id=?", Long.class, id);
        if (previous == null) throw new IllegalStateException("Phân bổ theo lịch thiếu phân bổ trước");
        return previous;
    }

    /** FR-011a: tới ngày mà giường mới chưa Trống, giữ Tương lai và chờ giường về Trống. */
    public void waitForBed(long id, Instant plannedAt) {
        jdbc.update(
                "UPDATE bed_allocation SET waiting_for_bed_ready=TRUE,planned_at=? WHERE id=?",
                Timestamp.from(plannedAt),
                id);
    }

    public void activateScheduled(long id, Instant at, Instant plannedAt, CheckedCareLevel level) {
        jdbc.update(
                "UPDATE bed_allocation SET status='ACTIVE',start_at=?,planned_at=?,waiting_for_bed_ready=FALSE,"
                        + "checked_care_level=?,care_level_source=? WHERE id=?",
                Timestamp.from(at),
                Timestamp.from(plannedAt),
                level.level().name(),
                level.source().name(),
                id);
    }

    /** FR-030: Hành chính đã lập yêu cầu sau chuyển gấp: gỡ dấu chờ lập yêu cầu, ngừng nhắc. */
    public void requestCreated(long id) {
        jdbc.update("UPDATE bed_allocation SET awaiting_request=FALSE WHERE id=?", id);
        reminders.end("BED_ALLOCATION", id, "AWAITING_REQUEST");
    }

    /** FR-030: yêu cầu sau chuyển gấp bị hủy, Áp dụng không thành: đặt lại dấu chờ lập yêu cầu, mở lại chu kỳ nhắc. */
    public boolean awaitingRequestAgain(long id, Instant at) {
        boolean changed = jdbc.update(
                        "UPDATE bed_allocation SET awaiting_request=TRUE WHERE id=? AND status='ACTIVE'"
                                + " AND awaiting_request=FALSE AND awaiting_same_price=FALSE",
                        id)
                == 1;
        if (changed) reminders.restart("BED_ALLOCATION", id, "AWAITING_REQUEST", at);
        return changed;
    }

    /** FR-030: yêu cầu bị từ chối: dấu "chờ chuyển về phòng cùng giá", nhắc mỗi CFG-M03-02. */
    public void awaitingSamePrice(long id, Instant at) {
        jdbc.update("UPDATE bed_allocation SET awaiting_same_price=TRUE WHERE id=?", id);
        reminders.restart("BED_ALLOCATION", id, "AWAITING_SAME_PRICE", at);
    }

    public java.util.Optional<Long> openedBy(long id) {
        return java.util.Optional.ofNullable(
                jdbc.queryForObject("SELECT opened_by_id FROM bed_allocation WHERE id=?", Long.class, id));
    }

    public void clearWithoutBed(long residentId) {
        positions.clearWithoutBed(residentId);
        reminders.end("RESIDENT", residentId, "WITHOUT_BED");
    }

    public void cancel(long id, String reason) {
        jdbc.update("UPDATE bed_allocation SET status='CANCELLED',cancellation_reason=? WHERE id=?", reason, id);
    }

    public boolean markUnsuitable(long id, boolean unsuitable) {
        boolean changed = jdbc.update(
                        "UPDATE bed_allocation SET room_unsuitable=? WHERE id=? AND status IN ('ACTIVE','FUTURE') AND room_unsuitable<>?",
                        unsuitable,
                        id,
                        unsuitable)
                > 0;
        if (changed && unsuitable) {
            reminders.restart("BED_ALLOCATION", id, "ROOM_UNSUITABLE", time.now());
        } else if (!unsuitable) {
            reminders.end("BED_ALLOCATION", id, "ROOM_UNSUITABLE");
        }
        return changed;
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
        reminders.endAll("BED_ALLOCATION", id);
    }

    /** FR-027 (e): dấu nhập bù kèm thời điểm thực hiện thật. */
    public void backfilled(long id, Instant recordedAt) {
        jdbc.update(
                "UPDATE bed_allocation SET backfilled=TRUE,actual_recorded_at=? WHERE id=?",
                Timestamp.from(recordedAt),
                id);
    }

    public void awaitingDoctor(long id) {
        jdbc.update(
                "UPDATE bed_allocation SET doctor_confirmation_requested_at="
                        + "CASE WHEN awaiting_doctor_confirmation THEN doctor_confirmation_requested_at ELSE ? END,"
                        + "awaiting_doctor_confirmation=TRUE WHERE id=?",
                Timestamp.from(time.now().truncatedTo(ChronoUnit.MICROS)),
                id);
    }

    public boolean markWithoutBed(long residentId, Instant at) {
        if (!positions.markWithoutBed(residentId, at)) return false;
        // Lần nhắc đầu là lúc đặt dấu (BR-M03-19), như reminder_count = 1 của bảng cũ.
        reminders.start("RESIDENT", residentId, "WITHOUT_BED", at);
        reminders.remind("RESIDENT", residentId, "WITHOUT_BED", at);
        return true;
    }
}
