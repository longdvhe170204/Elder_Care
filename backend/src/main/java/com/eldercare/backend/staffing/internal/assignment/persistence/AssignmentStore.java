package com.eldercare.backend.staffing.internal.assignment.persistence;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.staffing.internal.assignment.domain.*;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
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
    private final com.eldercare.backend.common.data.TechnicalLocks locks;

    public AssignmentStore(JdbcTemplate jdbc, com.eldercare.backend.common.data.TechnicalLocks locks) {
        this.jdbc = jdbc;
        this.locks = locks;
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
            r.getObject("legacy_floor_manager_id", Long.class) == null
                    ? r.getLong("id")
                    : r.getLong("legacy_floor_manager_id"),
            r.getLong("staff_id"),
            r.getLong("floor_area_id"),
            at(r, "starts_at"),
            at(r, "ends_at"),
            r.getString("status"),
            r.getString("reason"));

    public void lockFloors(Collection<Long> ids) {
        ids.stream().distinct().sorted().forEach(id -> locks.lock("M09.TANG:" + id));
    }

    public StaffAssignment find(long id, boolean lock) {
        return jdbc
                .query(
                        "SELECT * FROM staff_assignment WHERE kind='CARE' AND id=?" + (lock ? " FOR UPDATE" : ""),
                        ROW,
                        id)
                .stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public List<StaffAssignment> inShift(long id, boolean lock) {
        return jdbc.query(
                "SELECT * FROM staff_assignment WHERE kind='CARE' AND shift_id=? ORDER BY id"
                        + (lock ? " FOR UPDATE" : ""),
                ROW,
                id);
    }

    public List<StaffAssignment> inSchedule(long id) {
        return jdbc.query(
                "SELECT a.* FROM staff_assignment a JOIN work_shift s ON s.id=a.shift_id WHERE s.schedule_id=? ORDER BY a.id",
                ROW,
                id);
    }

    public List<StaffAssignment> directResident(long resident, boolean lock) {
        return jdbc.query(
                "SELECT * FROM staff_assignment WHERE kind='CARE' AND target_type='RESIDENT' AND target_id=? AND status IN ('SCHEDULED','ACTIVE') ORDER BY id"
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
        jdbc.update("DELETE FROM staff_assignment WHERE kind='CARE' AND id=?", id);
    }

    public void updateDraft(StaffAssignment before, StaffAssignment value) {
        int updated = jdbc.update(
                "UPDATE staff_assignment SET staff_id=?,target_type=?,target_id=?,task_type_id=?,role=?,assignment_kind=?,assigned_nurse=?,starts_at=?,ends_at=?,version=version+1 WHERE kind='CARE' AND id=? AND version=? AND status='DRAFT'",
                value.staffId(),
                value.targetType(),
                value.targetId(),
                value.taskTypeId(),
                value.role().name(),
                value.assignmentKind(),
                value.assignedNurse(),
                ShiftStore.sql(value.startsAt()),
                ShiftStore.sql(value.endsAt()),
                before.id(),
                before.version());
        if (updated != 1)
            throw new com.eldercare.backend.common.error.VersionMismatchException(find(before.id(), false));
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
                "SELECT * FROM staff_assignment WHERE kind='CARE' AND ((status='SCHEDULED' AND starts_at<=?) OR (status='ACTIVE' AND ends_at<=?)) ORDER BY staff_id,id",
                ROW,
                ShiftStore.sql(at),
                ShiftStore.sql(at));
    }

    public List<FloorManagerAssignment> managers(Long floor, boolean lock) {
        return jdbc.query(
                "SELECT * FROM staff_assignment WHERE kind='FLOOR_MANAGER' AND (? IS NULL OR floor_area_id=?) ORDER BY floor_area_id,starts_at,id"
                        + (lock ? " FOR UPDATE" : ""),
                FLOOR,
                floor,
                floor);
    }

    public FloorManagerAssignment manager(long id, boolean lock) {
        return jdbc
                .query(
                        "SELECT * FROM staff_assignment WHERE kind='FLOOR_MANAGER' AND COALESCE(legacy_floor_manager_id,id)=?"
                                + (lock ? " FOR UPDATE" : ""),
                        FLOOR,
                        id)
                .stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public List<FloorManagerAssignment> managersForStaff(long staff, Instant from, Instant to) {
        return jdbc.query(
                "SELECT * FROM staff_assignment WHERE kind='FLOOR_MANAGER' AND staff_id=? AND starts_at<? AND (ends_at IS NULL OR ends_at>?) ORDER BY starts_at,id",
                FLOOR,
                staff,
                ShiftStore.sql(to),
                ShiftStore.sql(from));
    }

    public long assignFloor(long staff, long floor, Instant from, Instant to, String reason, long actor, Instant now) {
        return insert(
                "INSERT INTO staff_assignment(kind,staff_id,floor_area_id,starts_at,ends_at,reason,assigned_by,created_at,status) VALUES ('FLOOR_MANAGER',?,?,?,?,?,?,?,'ACTIVE')",
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
                "UPDATE staff_assignment SET ends_at=?,status=?,reason=?,version=version+1 WHERE kind='FLOOR_MANAGER' AND COALESCE(legacy_floor_manager_id,id)=?",
                ShiftStore.sql(end),
                ended ? "ENDED" : "ACTIVE",
                reason,
                row.id());
    }

    public List<FloorManagerAssignment> expiringManagers(Instant at) {
        return jdbc.query(
                "SELECT * FROM staff_assignment WHERE kind='FLOOR_MANAGER' AND status='ACTIVE' AND ends_at<=? ORDER BY staff_id,id",
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

    public List<Long> activeEndingAfter(long staffId, Instant after) {
        return jdbc.queryForList(
                "SELECT id FROM staff_assignment WHERE kind='CARE' AND staff_id=? AND status IN ('ACTIVE','SCHEDULED') AND ends_at>? ORDER BY id",
                Long.class,
                staffId,
                com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore.sql(after));
    }

    public List<Long> activeTaskTypesEndingAfter(long staffId, Instant after) {
        return jdbc.queryForList(
                "SELECT DISTINCT task_type_id FROM staff_assignment WHERE kind='CARE' AND staff_id=? AND status IN ('ACTIVE','SCHEDULED') AND task_type_id IS NOT NULL AND ends_at>?",
                Long.class,
                staffId,
                com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore.sql(after));
    }

    public List<Long> activeManagerAssignmentsForStaff(long staffId, Instant at) {
        return jdbc.queryForList(
                "SELECT COALESCE(legacy_floor_manager_id,id) FROM staff_assignment WHERE kind='FLOOR_MANAGER' AND staff_id=? AND status='ACTIVE' AND (ends_at IS NULL OR ends_at>?) ORDER BY floor_area_id,id",
                Long.class,
                staffId,
                com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore.sql(at));
    }
}
