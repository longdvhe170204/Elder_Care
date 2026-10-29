package com.eldercare.backend.staffing.shift.internal.persistence;

import com.eldercare.backend.staffing.shift.internal.domain.StaffAbsence;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostPublicationStore {
    private final JdbcTemplate jdbc;

    public PostPublicationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<StaffAbsence> absence(long id) {
        return jdbc
                .query(
                        "SELECT * FROM shift_staff WHERE id=? AND absent_at IS NOT NULL",
                        (r, i) -> new StaffAbsence(
                                r.getLong("id"),
                                r.getLong("shift_id"),
                                r.getLong("staff_id"),
                                r.getObject("absence_recorded_by", Long.class),
                                r.getObject("absent_at", LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                        id)
                .stream()
                .findFirst();
    }

    public long memberId(long shift, long staff) {
        return jdbc.queryForObject(
                "SELECT id FROM shift_staff WHERE shift_id=? AND staff_id=?", Long.class, shift, staff);
    }

    public void add(long shift, long staff, Instant at, String entry, String reason) {
        jdbc.update(
                "INSERT INTO shift_staff(shift_id,staff_id,added_at,entry_basis,addition_reason) VALUES (?,?,?,?,?)",
                shift,
                staff,
                ShiftStore.sql(at),
                entry,
                reason);
        bump(shift);
    }

    public void absent(long shift, long staff, Instant at, String reason, long actor) {
        jdbc.update(
                "UPDATE shift_staff SET absent_at=?,absence_reason=?,absence_recorded_by=? WHERE shift_id=? AND staff_id=?",
                ShiftStore.sql(at),
                reason,
                actor,
                shift,
                staff);
        bump(shift);
    }

    public void changeLead(long shift, Long staff, Instant at, String basis, long actor, String reason) {
        jdbc.update(
                "UPDATE shift_lead_history SET ends_at=GREATEST(starts_at,?) WHERE shift_id=? AND ends_at IS NULL",
                ShiftStore.sql(at),
                shift);
        jdbc.update(
                "UPDATE work_shift SET lead_staff_id=?,missing_lead=?,version=version+1 WHERE id=?",
                staff,
                staff == null,
                shift);
        if (staff != null)
            jdbc.update(
                    "INSERT INTO shift_lead_history(shift_id,staff_id,starts_at,basis,actor_id,reason) VALUES (?,?,?,?,?,?)",
                    shift,
                    staff,
                    ShiftStore.sql(at),
                    basis,
                    actor,
                    reason);
    }

    public Optional<Long> reassignDraft(long shift, long staff) {
        var ids = jdbc.queryForList(
                "SELECT id FROM shift_handover WHERE shift_id=? AND status IN ('DRAFT','FEEDBACK') FOR UPDATE",
                Long.class,
                shift);
        if (ids.isEmpty()) return Optional.empty();
        long id = ids.getFirst();
        jdbc.update("UPDATE shift_handover SET handover_staff_id=?,version=version+1 WHERE id=?", staff, id);
        return Optional.of(id);
    }

    public void cancel(long shift, String reason, Instant at) {
        jdbc.update(
                "UPDATE shift_lead_history SET ends_at=GREATEST(starts_at,?) WHERE shift_id=? AND ends_at IS NULL",
                ShiftStore.sql(at),
                shift);
        jdbc.update(
                "UPDATE work_shift SET status='CANCELLED',cancellation_reason=?,version=version+1 WHERE id=?",
                reason,
                shift);
    }

    public void leave(long shift, long staff, Instant at, String basis, Long leaveRequest) {
        jdbc.update(
                "UPDATE shift_staff SET left_at=?,departure_basis=?,approved_leave_request_id=? WHERE shift_id=? AND staff_id=?",
                ShiftStore.sql(at),
                basis,
                leaveRequest,
                shift,
                staff);
        bump(shift);
    }

    private void bump(long shift) {
        jdbc.update("UPDATE work_shift SET version=version+1 WHERE id=?", shift);
    }
}
