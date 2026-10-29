package com.eldercare.backend.staffing.staff.internal.persistence;

import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

/** T033: metadata yêu cầu nghỉ việc và biến phiên đổi trạng thái cùng connection; chỉ bảng M09. */
@Repository
public class StaffEmploymentStore {
    private final JdbcTemplate jdbc;

    public StaffEmploymentStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void request(long id, Instant effective, String reason) {
        jdbc.update(
                "UPDATE staff_profile SET employment_ends_at=?,termination_reason=?,termination_applied_at=NULL,version=version+1 WHERE id=?",
                ShiftStore.sql(effective),
                reason,
                id);
    }

    public List<Long> due(Instant at) {
        return jdbc.queryForList(
                "SELECT id FROM staff_profile WHERE status='WORKING' AND employment_ends_at<=? AND termination_applied_at IS NULL ORDER BY id",
                Long.class,
                ShiftStore.sql(at));
    }

    public Instant effectiveAt(long id) {
        return jdbc.queryForObject(
                        "SELECT employment_ends_at FROM staff_profile WHERE id=?", java.sql.Timestamp.class, id)
                .toInstant();
    }

    public List<Long> affectedShifts(long id, Instant at) {
        return jdbc.queryForList(
                "SELECT s.id FROM work_shift s JOIN shift_staff m ON m.shift_id=s.id WHERE m.staff_id=? AND s.ends_at>? AND s.status<>'CANCELLED' ORDER BY s.id",
                Long.class,
                id,
                ShiftStore.sql(at));
    }

    public void withdraw(long person, long shift, Instant at) {
        jdbc.update(
                "UPDATE shift_staff SET withdrawn_on_resignation_at=? WHERE staff_id=? AND shift_id=? AND (withdrawn_on_resignation_at IS NULL OR withdrawn_on_resignation_at>?)",
                ShiftStore.sql(at),
                person,
                shift,
                ShiftStore.sql(at));
        jdbc.update(
                "UPDATE work_shift SET missing_lead=TRUE,version=version+1 WHERE id=? AND lead_staff_id=?",
                shift,
                person);
        jdbc.update(
                "UPDATE shift_lead_history SET ends_at=GREATEST(starts_at,?) WHERE shift_id=? AND staff_id=? AND ends_at IS NULL",
                ShiftStore.sql(at),
                shift,
                person);
    }

    public void status(long id, String status, Instant actual) {
        jdbc.execute((ConnectionCallback<Void>) c -> {
            try (var sql = c.createStatement()) {
                sql.execute("SET @staff_status_command=1");
                try (var update = c.prepareStatement(
                        "UPDATE staff_profile SET status=?,termination_applied_at=?,employment_ends_at=IF(?='WORKING',NULL,employment_ends_at),termination_reason=IF(?='WORKING',NULL,termination_reason),version=version+1 WHERE id=?")) {
                    update.setString(1, status);
                    update.setObject(2, "RESIGNED".equals(status) ? ShiftStore.sql(actual) : null);
                    update.setString(3, status);
                    update.setString(4, status);
                    update.setLong(5, id);
                    update.executeUpdate();
                } finally {
                    sql.execute("SET @staff_status_command=0");
                }
            }
            return null;
        });
    }
}
