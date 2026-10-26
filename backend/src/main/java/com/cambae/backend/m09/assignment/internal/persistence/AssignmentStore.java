package com.cambae.backend.m09.assignment.internal.persistence;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m09.assignment.internal.domain.*;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** T019: chỉ ba bảng phân công M09; mọi tập khóa lấy theo id tăng. */
@Repository
public class AssignmentStore {
    private final JdbcTemplate jdbc;

    public AssignmentStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Instant at(ResultSet r, String name) throws SQLException {
        var v = r.getObject(name, LocalDateTime.class);
        return v == null ? null : v.toInstant(ZoneOffset.UTC);
    }

    private static final RowMapper<StaffAssignment> ROW = (r, i) -> new StaffAssignment(
            r.getLong("id"),
            r.getLong("shift_id"),
            r.getLong("staff_id"),
            r.getString("target_type"),
            r.getLong("target_id"),
            r.getObject("task_type_id", Long.class),
            Role.valueOf(r.getString("role")),
            r.getString("assignment_kind"),
            r.getBoolean("assigned_nurse"),
            at(r, "starts_at"),
            at(r, "ends_at"),
            r.getString("status"),
            at(r, "actual_end_at"),
            r.getString("source"),
            r.getLong("version"));
    private static final RowMapper<FloorManagerAssignment> FLOOR = (r, i) -> new FloorManagerAssignment(
            r.getLong("id"),
            r.getLong("staff_id"),
            r.getLong("floor_area_id"),
            at(r, "starts_at"),
            at(r, "ends_at"),
            r.getString("status"),
            r.getString("reason"));

    public void lockFloors(Collection<Long> ids) {
        ids.stream().distinct().sorted().forEach(id -> {
            jdbc.update("INSERT IGNORE INTO floor_scope_lock(floor_area_id) VALUES (?)", id);
            jdbc.queryForObject(
                    "SELECT floor_area_id FROM floor_scope_lock WHERE floor_area_id=? FOR UPDATE", Long.class, id);
        });
    }

    public StaffAssignment find(long id, boolean lock) {
        return jdbc.query("SELECT * FROM staff_assignment WHERE id=?" + (lock ? " FOR UPDATE" : ""), ROW, id).stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public List<StaffAssignment> inShift(long id, boolean lock) {
        return jdbc.query(
                "SELECT * FROM staff_assignment WHERE shift_id=? ORDER BY id" + (lock ? " FOR UPDATE" : ""), ROW, id);
    }

    public List<StaffAssignment> inSchedule(long id) {
        return jdbc.query(
                "SELECT a.* FROM staff_assignment a JOIN work_shift s ON s.id=a.shift_id WHERE s.schedule_id=? ORDER BY a.id",
                ROW,
                id);
    }

    public List<StaffAssignment> directResident(long resident, boolean lock) {
        return jdbc.query(
                "SELECT * FROM staff_assignment WHERE target_type='RESIDENT' AND target_id=? AND status IN ('SCHEDULED','ACTIVE') ORDER BY id"
                        + (lock ? " FOR UPDATE" : ""),
                ROW,
                resident);
    }

    public List<StaffAssignment> duplicateCandidates(StaffAssignment a, Long exclude, boolean lock) {
        return jdbc.query(
                "SELECT a.* FROM staff_assignment a JOIN work_shift s ON s.id=a.shift_id WHERE a.target_type=? AND a.target_id=? AND a.role=? AND a.assignment_kind='PRIMARY' AND a.task_type_id<=>? AND a.status<>'CANCELLED' AND s.status<>'CANCELLED' AND a.starts_at<? AND COALESCE(a.actual_end_at,a.ends_at)>? AND (? IS NULL OR a.id<>?) ORDER BY a.id"
                        + (lock ? " FOR UPDATE" : ""),
                ROW,
                a.targetType(),
                a.targetId(),
                a.role().name(),
                a.taskTypeId(),
                ShiftStore.sql(a.endsAt()),
                ShiftStore.sql(a.startsAt()),
                exclude,
                exclude);
    }

    public long insert(StaffAssignment a, long actor, Instant now) {
        return insert(
                "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,task_type_id,role,assignment_kind,assigned_nurse,starts_at,ends_at,status,source,created_at,created_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                a.shiftId(),
                a.staffId(),
                a.targetType(),
                a.targetId(),
                a.taskTypeId(),
                a.role().name(),
                a.assignmentKind(),
                a.assignedNurse(),
                ShiftStore.sql(a.startsAt()),
                ShiftStore.sql(a.endsAt()),
                a.status(),
                a.source(),
                ShiftStore.sql(now),
                actor);
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM staff_assignment WHERE id=?", id);
    }

    public void removeDrafts(long shift, Collection<Long> remaining) {
        for (var a : inShift(shift, true))
            if ("DRAFT".equals(a.status()) && !remaining.contains(a.staffId())) delete(a.id());
    }

    public void transition(StaffAssignment a, String status, Instant actualEnd, String reason) {
        jdbc.update(
                "UPDATE staff_assignment SET status=?,actual_end_at=?,end_reason=?,version=version+1 WHERE id=?",
                status,
                ShiftStore.sql(actualEnd),
                reason,
                a.id());
    }

    public List<StaffAssignment> atTargets(Set<PrimaryAssigneeResolver.TargetRef> targets, Instant at) {
        return matchingTargets(targets, at, at, true);
    }

    public List<StaffAssignment> effectiveTargets(
            Set<PrimaryAssigneeResolver.TargetRef> targets, Instant from, Instant to) {
        return matchingTargets(targets, from, to, false);
    }

    private List<StaffAssignment> matchingTargets(
            Set<PrimaryAssigneeResolver.TargetRef> targets, Instant from, Instant to, boolean point) {
        if (targets.isEmpty()) return List.of();
        var args = new ArrayList<Object>();
        var clauses = new ArrayList<String>();
        for (var target : targets) {
            clauses.add("(a.target_type=? AND a.target_id=?)");
            args.add(target.type());
            args.add(target.id());
        }
        args.add(ShiftStore.sql(point ? from : to));
        args.add(ShiftStore.sql(from));
        return jdbc.query(
                "SELECT a.* FROM staff_assignment a JOIN work_shift s ON s.id=a.shift_id WHERE ("
                        + String.join(" OR ", clauses)
                        + ") AND a.status NOT IN ('DRAFT','CANCELLED') AND s.status NOT IN ('DRAFT','CANCELLED') AND a.starts_at"
                        + (point ? "<=?" : "<?") + " AND COALESCE(a.actual_end_at,a.ends_at)>? ORDER BY a.id",
                ROW,
                args.toArray());
    }

    public List<StaffAssignment> history(long staff, Instant from, Instant to) {
        return jdbc.query(
                "SELECT a.* FROM staff_assignment a JOIN work_shift s ON s.id=a.shift_id WHERE a.staff_id=? AND a.status NOT IN ('DRAFT','CANCELLED') AND s.status NOT IN ('DRAFT','CANCELLED') AND s.starts_at<? AND s.ends_at>? ORDER BY a.starts_at,a.id",
                ROW,
                staff,
                ShiftStore.sql(to),
                ShiftStore.sql(from));
    }

    public List<StaffAssignment> due(Instant at) {
        return jdbc.query(
                "SELECT * FROM staff_assignment WHERE (status='SCHEDULED' AND starts_at<=?) OR (status='ACTIVE' AND ends_at<=?) ORDER BY staff_id,id",
                ROW,
                ShiftStore.sql(at),
                ShiftStore.sql(at));
    }

    public List<FloorManagerAssignment> managers(Long floor, boolean lock) {
        return jdbc.query(
                "SELECT * FROM floor_manager_assignment WHERE (? IS NULL OR floor_area_id=?) ORDER BY floor_area_id,starts_at,id"
                        + (lock ? " FOR UPDATE" : ""),
                FLOOR,
                floor,
                floor);
    }

    public FloorManagerAssignment manager(long id, boolean lock) {
        return jdbc
                .query("SELECT * FROM floor_manager_assignment WHERE id=?" + (lock ? " FOR UPDATE" : ""), FLOOR, id)
                .stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public List<FloorManagerAssignment> managersForStaff(long staff, Instant from, Instant to) {
        return jdbc.query(
                "SELECT * FROM floor_manager_assignment WHERE staff_id=? AND starts_at<? AND (ends_at IS NULL OR ends_at>?) ORDER BY starts_at,id",
                FLOOR,
                staff,
                ShiftStore.sql(to),
                ShiftStore.sql(from));
    }

    public long assignFloor(long staff, long floor, Instant from, Instant to, String reason, long actor, Instant now) {
        return insert(
                "INSERT INTO floor_manager_assignment(staff_id,floor_area_id,starts_at,ends_at,reason,assigned_by,created_at) VALUES (?,?,?,?,?,?,?)",
                staff,
                floor,
                ShiftStore.sql(from),
                ShiftStore.sql(to),
                reason,
                actor,
                ShiftStore.sql(now));
    }

    public void shorten(FloorManagerAssignment row, Instant end, boolean ended, String reason) {
        jdbc.update(
                "UPDATE floor_manager_assignment SET ends_at=?,status=?,reason=?,version=version+1 WHERE id=?",
                ShiftStore.sql(end),
                ended ? "ENDED" : "ACTIVE",
                reason,
                row.id());
    }

    public List<FloorManagerAssignment> expiringManagers(Instant at) {
        return jdbc.query(
                "SELECT * FROM floor_manager_assignment WHERE status='ACTIVE' AND ends_at<=? ORDER BY staff_id,id",
                FLOOR,
                ShiftStore.sql(at));
    }

    public List<Long> references(String type, long id, boolean active) {
        return jdbc.queryForList(
                "SELECT a.id FROM staff_assignment a JOIN work_shift s ON s.id=a.shift_id WHERE a.target_type=? AND a.target_id=?"
                        + (active ? " AND a.status NOT IN ('ENDED','CANCELLED') AND s.status<>'CANCELLED'" : ""),
                Long.class,
                type,
                id);
    }

    private long insert(String sql, Object... args) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }
}
