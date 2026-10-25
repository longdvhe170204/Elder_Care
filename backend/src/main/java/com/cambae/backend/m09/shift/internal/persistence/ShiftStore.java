package com.cambae.backend.m09.shift.internal.persistence;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m09.shift.internal.domain.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Chỉ bảng M09; đọc hiện hành FOR UPDATE dùng sau khóa nhân viên để chặn chồng ca đồng thời. */
@Repository
public class ShiftStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public ShiftStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public static LocalDateTime sql(Instant at) {
        return at == null ? null : LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet r, String col) throws SQLException {
        var d = r.getObject(col, LocalDateTime.class);
        return d == null ? null : d.toInstant(ZoneOffset.UTC);
    }

    private static final RowMapper<ShiftTemplate> TEMPLATE = (r, i) -> new ShiftTemplate(
            r.getLong("id"),
            r.getString("name"),
            r.getObject("start_time", LocalTime.class),
            r.getObject("end_time", LocalTime.class),
            r.getString("scope_type"),
            r.getBoolean("requires_handover"),
            "ACTIVE".equals(r.getString("status")),
            r.getLong("version"));
    private static final RowMapper<ShiftSchedule> SCHEDULE = (r, i) -> new ShiftSchedule(
            r.getLong("id"),
            YearMonth.parse(r.getString("month")),
            r.getString("scope_type"),
            r.getObject("scope_id", Long.class),
            r.getString("status"),
            r.getObject("created_by", Long.class),
            r.getLong("version"));
    private static final RowMapper<WorkShift> SHIFT = (r, i) -> new WorkShift(
            r.getLong("id"),
            r.getLong("schedule_id"),
            r.getLong("template_id"),
            instant(r, "starts_at"),
            instant(r, "ends_at"),
            r.getString("scope_type"),
            r.getObject("scope_id", Long.class),
            r.getBoolean("requires_handover"),
            r.getString("status"),
            r.getObject("lead_staff_id", Long.class),
            r.getBoolean("missing_lead"),
            r.getLong("version"));

    public List<ShiftTemplate> templates() {
        return jdbc.query("SELECT * FROM shift_template ORDER BY id", TEMPLATE);
    }

    public ShiftTemplate template(long id, boolean lock) {
        return jdbc
                .query("SELECT * FROM shift_template WHERE id=?" + (lock ? " FOR UPDATE" : ""), TEMPLATE, id)
                .stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public long createTemplate(TemplateInput input, boolean handover, Instant at) {
        return insert(
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,requires_handover,status,created_at) VALUES (?,?,?,?,?,?,?)",
                input.name().strip(),
                input.startTime(),
                input.endTime(),
                input.scopeType(),
                handover,
                input.active() ? "ACTIVE" : "INACTIVE",
                sql(at));
    }

    public void updateTemplate(long id, TemplateInput input, boolean handover) {
        jdbc.update(
                "UPDATE shift_template SET name=?,start_time=?,end_time=?,scope_type=?,requires_handover=?,status=?,version=version+1 WHERE id=?",
                input.name().strip(),
                input.startTime(),
                input.endTime(),
                input.scopeType(),
                handover,
                input.active() ? "ACTIVE" : "INACTIVE",
                id);
    }

    public long createSchedule(YearMonth month, String scope, Long id, long actor, Instant at) {
        return insert(
                "INSERT INTO shift_schedule(month,scope_type,scope_id,created_by,created_at) VALUES (?,?,?,?,?)",
                month.toString(),
                scope,
                id,
                actor,
                sql(at));
    }

    public List<ShiftSchedule> schedules(YearMonth month, Long scope) {
        return scope == null
                ? jdbc.query("SELECT * FROM shift_schedule WHERE month=? ORDER BY id", SCHEDULE, month.toString())
                : jdbc.query(
                        "SELECT * FROM shift_schedule WHERE month=? AND scope_id=? ORDER BY id",
                        SCHEDULE,
                        month.toString(),
                        scope);
    }

    public ShiftSchedule schedule(long id, boolean lock) {
        return jdbc
                .query("SELECT * FROM shift_schedule WHERE id=?" + (lock ? " FOR UPDATE" : ""), SCHEDULE, id)
                .stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public WorkShift shift(long id, boolean lock) {
        return jdbc.query("SELECT * FROM work_shift WHERE id=?" + (lock ? " FOR UPDATE" : ""), SHIFT, id).stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public Optional<WorkShift> find(long id) {
        return jdbc.query("SELECT * FROM work_shift WHERE id=?", SHIFT, id).stream()
                .findFirst();
    }

    public List<WorkShift> atFloor(long floor, Instant at) {
        return jdbc.query(
                "SELECT * FROM work_shift WHERE scope_type IN ('FLOOR','AREA') AND scope_id=? AND status NOT IN ('DRAFT','CANCELLED') AND starts_at<=? AND ends_at>? ORDER BY starts_at,id",
                SHIFT,
                floor,
                sql(at),
                sql(at));
    }

    public Optional<WorkShift> nextAtFloor(long floor, Instant at) {
        return jdbc
                .query(
                        "SELECT * FROM work_shift WHERE scope_type IN ('FLOOR','AREA') AND scope_id=? AND status NOT IN ('DRAFT','CANCELLED') AND starts_at>? ORDER BY starts_at,id LIMIT 1",
                        SHIFT,
                        floor,
                        sql(at))
                .stream()
                .findFirst();
    }

    public List<WorkShift> floorRange(long floor, Instant from, Instant to) {
        return jdbc.query(
                "SELECT * FROM work_shift WHERE scope_type IN ('FLOOR','AREA') AND scope_id=? AND status NOT IN ('DRAFT','CANCELLED') AND starts_at<? AND ends_at>? ORDER BY starts_at,id",
                SHIFT,
                floor,
                sql(to),
                sql(from));
    }

    public List<WorkShift> at(Instant at, boolean facilityOnly) {
        return jdbc.query(
                "SELECT * FROM work_shift WHERE status NOT IN ('DRAFT','CANCELLED') AND starts_at<=? AND ends_at>?"
                        + (facilityOnly ? " AND scope_type='FACILITY'" : "") + " ORDER BY starts_at,id",
                SHIFT,
                sql(at),
                sql(at));
    }

    public List<WorkShift> due(Instant at) {
        return jdbc.query(
                "SELECT * FROM work_shift WHERE (status='PUBLISHED' AND starts_at<=?) OR (status='IN_PROGRESS' AND ends_at<=?) OR status='AWAITING_HANDOVER' ORDER BY starts_at,id",
                SHIFT,
                sql(at),
                sql(at));
    }

    private static final String MEMBER_FROM = "GREATEST(s.starts_at,COALESCE(m.added_at,s.starts_at))";
    private static final String MEMBER_TO =
            "LEAST(s.ends_at,COALESCE(m.absent_at,s.ends_at),COALESCE(m.withdrawn_on_resignation_at,s.ends_at),COALESCE(m.left_at,s.ends_at))";

    public List<WorkShift> staffRange(long staff, Instant from, Instant to, boolean effectiveMembership) {
        String extra = effectiveMembership
                ? " AND m.approved_leave_request_id IS NULL AND " + MEMBER_FROM + "<" + MEMBER_TO + " AND "
                        + MEMBER_FROM + "<? AND " + MEMBER_TO + ">?"
                : "";
        Object[] args = effectiveMembership
                ? new Object[] {staff, sql(to), sql(from), sql(to), sql(from)}
                : new Object[] {staff, sql(to), sql(from)};
        return jdbc.query(
                "SELECT s.* FROM shift_staff m JOIN work_shift s ON s.id=m.shift_id WHERE m.staff_id=? AND s.status NOT IN ('DRAFT','CANCELLED') AND s.starts_at<? AND s.ends_at>?"
                        + extra + " ORDER BY s.starts_at,s.id",
                SHIFT,
                args);
    }

    public List<WorkShift> memberAt(long staff, Instant at) {
        return jdbc.query(
                "SELECT s.* FROM shift_staff m JOIN work_shift s ON s.id=m.shift_id WHERE m.staff_id=? AND s.status NOT IN ('DRAFT','CANCELLED') AND s.starts_at<=? AND s.ends_at>? AND m.approved_leave_request_id IS NULL AND "
                        + MEMBER_FROM + "<=? AND " + MEMBER_TO + ">? ORDER BY s.starts_at,s.id",
                SHIFT,
                staff,
                sql(at),
                sql(at),
                sql(at),
                sql(at));
    }

    public List<WorkShift> locationReferences(String type, long id, boolean activeOnly) {
        return jdbc.query(
                "SELECT * FROM work_shift WHERE scope_type=? AND scope_id=?"
                        + (activeOnly ? " AND status NOT IN ('CLOSED','CANCELLED')" : "") + " ORDER BY id",
                SHIFT,
                type,
                id);
    }

    public List<WorkShift> scheduleShifts(long id) {
        return jdbc.query("SELECT * FROM work_shift WHERE schedule_id=? ORDER BY starts_at,id", SHIFT, id);
    }

    public List<WorkShift> reportShifts(Instant from, Instant to) {
        return jdbc.query(
                "SELECT * FROM work_shift WHERE status NOT IN ('DRAFT','CANCELLED') AND starts_at<? AND ends_at>? ORDER BY starts_at,id",
                SHIFT,
                sql(to),
                sql(from));
    }

    public List<ShiftMember> members(long id) {
        return members(id, false);
    }

    public List<ShiftMember> members(long id, boolean lock) {
        return jdbc.query(
                "SELECT * FROM shift_staff WHERE shift_id=? ORDER BY staff_id" + (lock ? " FOR UPDATE" : ""),
                (r, i) -> new ShiftMember(
                        r.getLong("staff_id"),
                        r.getBoolean("on_duty_doctor"),
                        instant(r, "absent_at"),
                        instant(r, "withdrawn_on_resignation_at"),
                        instant(r, "added_at"),
                        r.getObject("approved_leave_request_id", Long.class),
                        instant(r, "left_at")),
                id);
    }

    public List<WorkShift> forStaff(long staff, Long exclude, boolean lock) {
        return jdbc.query(
                "SELECT s.*," + MEMBER_FROM + " AS effective_from," + MEMBER_TO
                        + " AS effective_to FROM work_shift s JOIN shift_staff m ON m.shift_id=s.id WHERE m.staff_id=? AND s.status<>'CANCELLED' AND (? IS NULL OR s.id<>?) AND m.approved_leave_request_id IS NULL AND "
                        + MEMBER_FROM + "<" + MEMBER_TO + " ORDER BY s.starts_at,s.id" + (lock ? " FOR UPDATE" : ""),
                (r, i) -> {
                    var s = SHIFT.mapRow(r, i);
                    return new WorkShift(
                            s.id(),
                            s.scheduleId(),
                            s.shiftTemplateId(),
                            instant(r, "effective_from"),
                            instant(r, "effective_to"),
                            s.scopeType(),
                            s.scopeId(),
                            s.handoverRequired(),
                            s.status(),
                            s.leadStaffId(),
                            s.missingLead(),
                            s.version());
                },
                staff,
                exclude,
                exclude);
    }

    public long createShift(
            ShiftSchedule schedule, ShiftTemplate template, ShiftInput input, Instant from, Instant to) {
        long id = insert(
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,lead_staff_id) VALUES (?,?,?,?,?,?,?,?)",
                schedule.id(),
                template.id(),
                sql(from),
                sql(to),
                schedule.scopeType(),
                schedule.scopeId(),
                template.handoverRequired(),
                input.leadStaffId());
        setMembers(id, input);
        bumpSchedule(schedule.id());
        return id;
    }

    public void updateShift(WorkShift before, ShiftTemplate template, ShiftInput input, Instant from, Instant to) {
        jdbc.update(
                "UPDATE work_shift SET template_id=?,starts_at=?,ends_at=?,requires_handover=?,lead_staff_id=?,version=version+1 WHERE id=?",
                template.id(),
                sql(from),
                sql(to),
                template.handoverRequired(),
                input.leadStaffId(),
                before.id());
        jdbc.update("DELETE FROM shift_staff WHERE shift_id=?", before.id());
        setMembers(before.id(), input);
        bumpSchedule(before.scheduleId());
    }

    private void setMembers(long id, ShiftInput input) {
        for (var member : input.staff())
            jdbc.update(
                    "INSERT INTO shift_staff(shift_id,staff_id,on_duty_doctor) VALUES (?,?,?)",
                    id,
                    member.staffId(),
                    member.onDutyDoctor());
    }

    public void deleteShift(WorkShift shift) {
        jdbc.update("DELETE FROM shift_staff WHERE shift_id=?", shift.id());
        jdbc.update("DELETE FROM work_shift WHERE id=?", shift.id());
        bumpSchedule(shift.scheduleId());
    }

    private void bumpSchedule(long id) {
        jdbc.update("UPDATE shift_schedule SET version=version+1 WHERE id=?", id);
    }

    public void publish(
            ShiftSchedule schedule, List<WorkShift> shifts, long actor, boolean self, String reason, Instant at) {
        jdbc.update(
                "UPDATE shift_schedule SET status='PUBLISHED',published_at=?,published_by=?,self_approved=?,publication_reason=?,version=version+1 WHERE id=?",
                sql(at),
                actor,
                self,
                reason,
                schedule.id());
        for (var shift : shifts) {
            jdbc.update("UPDATE work_shift SET status='PUBLISHED',version=version+1 WHERE id=?", shift.id());
            if (shift.leadStaffId() != null)
                jdbc.update(
                        "INSERT INTO shift_lead_history(shift_id,staff_id,starts_at,basis,actor_id) VALUES (?,?,?,'SCHEDULED',?)",
                        shift.id(),
                        shift.leadStaffId(),
                        sql(shift.startsAt()),
                        actor);
        }
    }

    public void savePublication(
            long scheduleId,
            Instant at,
            List<com.cambae.backend.m09.assignment.internal.domain.StaffAssignment> assignments) {
        var snapshots = scheduleShifts(scheduleId).stream()
                .map(s -> Map.of("shift", s, "staff", members(s.id())))
                .toList();
        insert(
                "INSERT INTO shift_publication(schedule_id,published_at,content) VALUES (?,?,?)",
                scheduleId,
                sql(at),
                json.writeValueAsString(Map.of(
                        "schedule", schedule(scheduleId, false), "shifts", snapshots, "assignments", assignments)));
    }

    public void transition(long id, String status) {
        jdbc.update("UPDATE work_shift SET status=?,version=version+1 WHERE id=?", status, id);
    }

    private long insert(String statement, Object... values) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }
}
