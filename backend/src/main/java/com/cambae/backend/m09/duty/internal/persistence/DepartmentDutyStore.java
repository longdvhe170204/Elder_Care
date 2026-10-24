package com.cambae.backend.m09.duty.internal.persistence;

import com.cambae.backend.m09.duty.internal.domain.DepartmentDuty;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Chỉ bảng M09; khóa bộ phận sau khi service khóa nhân viên theo thứ tự id. */
@Repository
public class DepartmentDutyStore {
    private final JdbcTemplate jdbc;

    public DepartmentDutyStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Long> lead(long id) {
        return jdbc
                .queryForList(
                        "SELECT lead_staff_id FROM department WHERE id=? AND status='ACTIVE' AND lead_staff_id IS NOT NULL",
                        Long.class,
                        id)
                .stream()
                .findFirst();
    }

    public List<DepartmentDuty> byLead(long person, boolean lock) {
        return jdbc.query(
                "SELECT id,name,lead_staff_id FROM department WHERE lead_staff_id=? AND status='ACTIVE' ORDER BY id"
                        + (lock ? " FOR UPDATE" : ""),
                (r, i) -> new DepartmentDuty(r.getLong("id"), r.getString("name"), r.getLong("lead_staff_id")),
                person);
    }

    public List<Long> departments(long person, Instant at) {
        return jdbc.queryForList(
                "SELECT department_id FROM staff_department WHERE staff_id=? AND starts_at<=? AND (ends_at IS NULL OR ends_at>?)",
                Long.class,
                person,
                ShiftStore.sql(at),
                ShiftStore.sql(at));
    }

    public void replace(long department, long replacement, Instant at, long actor, String reason) {
        jdbc.update(
                "UPDATE department_lead_history SET ends_at=? WHERE department_id=? AND ends_at IS NULL",
                ShiftStore.sql(at),
                department);
        jdbc.update("UPDATE department SET lead_staff_id=?,version=version+1 WHERE id=?", replacement, department);
        jdbc.update(
                "INSERT INTO department_lead_history(department_id,staff_id,starts_at,actor_id,reason) VALUES (?,?,?,?,?)",
                department,
                replacement,
                ShiftStore.sql(at),
                actor,
                reason);
    }
}
