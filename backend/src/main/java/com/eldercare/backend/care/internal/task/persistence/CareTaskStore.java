package com.eldercare.backend.care.internal.task.persistence;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.care.internal.task.domain.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** SQL chỉ trên care_task; snapshot typed, lịch sử do API bảng chung ghi. */
@Repository
public class CareTaskStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public CareTaskStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    private LocalDateTime local(Instant at) {
        return at == null ? null : LocalDateTime.ofInstant(at, BusinessTime.ZONE);
    }

    private Instant instant(ResultSet rs, String field) throws SQLException {
        var at = rs.getObject(field, LocalDateTime.class);
        return at == null ? null : at.atZone(BusinessTime.ZONE).toInstant();
    }

    private CareTask row(ResultSet r, int index) throws SQLException {
        return new CareTask(
                r.getLong("id"),
                r.getObject("resident_id", Long.class),
                r.getString("location_type"),
                r.getObject("location_id", Long.class),
                r.getLong("task_type_id"),
                SourceType.valueOf(r.getString("basis_type")),
                r.getObject("basis_id", Long.class),
                r.getObject("plan_id", Long.class),
                r.getObject("original_task_id", Long.class),
                r.getLong("shift_id"),
                r.getLong("floor_area_id"),
                instant(r, "planned_at"),
                instant(r, "window_from"),
                instant(r, "window_to"),
                Importance.valueOf(r.getString("importance")),
                Role.valueOf(r.getString("performer_role")),
                r.getObject("performer_id", Long.class),
                TaskStatus.valueOf(r.getString("status")),
                json.readValue(r.getString("data"), CareTaskData.class),
                r.getLong("created_by"),
                instant(r, "created_at"),
                r.getInt("version"),
                r.getBoolean("awaiting_remeasurement"),
                r.getString("carryover"));
    }

    public CareTask get(long id, boolean lock) {
        return jdbc.query("SELECT * FROM care_task WHERE id=?" + (lock ? " FOR UPDATE" : ""), this::row, id).stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public List<CareTask> inShift(long id) {
        return jdbc.query("SELECT * FROM care_task WHERE shift_id=? ORDER BY planned_at,id", this::row, id);
    }

    public List<CareTask> checklist(long shiftId, long floor, long staffId, Instant from) {
        return jdbc.query(
                "SELECT * FROM care_task WHERE floor_area_id=? AND ((planned_at>=? AND shift_id=?) OR carryover IS NOT NULL) AND (performer_id=? OR (performer_id IS NULL AND status IN('NOT_YET_DUE','DUE','OVERDUE'))) ORDER BY (status='OVERDUE') DESC,planned_at,id",
                this::row,
                floor,
                local(from),
                shiftId,
                staffId);
    }

    public CareTask claim(CareTask task, long staffId) {
        int updated = jdbc.update(
                "UPDATE care_task SET performer_id=?,version=version+1 WHERE id=? AND version=? AND status IN('NOT_YET_DUE','DUE','OVERDUE')",
                staffId,
                task.id(),
                task.version());
        if (updated != 1)
            throw new com.eldercare.backend.common.error.BusinessException(
                    org.springframework.http.HttpStatus.CONFLICT, "CARE_TASK_CLOSED", "Công việc đã thay đổi");
        return get(task.id(), false);
    }

    public List<CareTask> notYetDueFrom(long resident, Instant at) {
        return jdbc.query(
                "SELECT * FROM care_task WHERE resident_id=? AND status='NOT_YET_DUE' AND planned_at>=? ORDER BY id",
                this::row,
                resident,
                local(at));
    }

    public List<CareTask> forSource(SourceType type, long id) {
        return jdbc.query(
                "SELECT * FROM care_task WHERE basis_type=? AND basis_id=? ORDER BY id", this::row, type.name(), id);
    }

    public List<CareTask> waiting(Instant now) {
        return jdbc.query(
                "SELECT * FROM care_task WHERE status='NOT_YET_DUE' AND window_from<=? ORDER BY resident_id,id",
                this::row,
                local(now));
    }

    public boolean duplicate(CareTask t) {
        if (t.basisType() == SourceType.AD_HOC
                || t.basisType() == SourceType.AD_HOC_RECORDING
                || t.basisType() == SourceType.REWORK) return false;
        boolean special = t.basisType() == SourceType.SHIFT_START_CHECK || t.basisType() == SourceType.SUPERVISION;
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM care_task WHERE status<>'CANCELLED' AND basis_type=? AND "
                        + (special ? "shift_id=? AND performer_id=?" : "basis_id=? AND resident_id=? AND planned_at=?")
                        + ")",
                Boolean.class,
                special
                        ? new Object[] {t.basisType().name(), t.shiftId(), t.performerId()}
                        : new Object[] {t.basisType().name(), t.basisId(), t.residentId(), local(t.plannedAt())}));
    }

    public boolean overlapsOldPlan(CareTask t) {
        if (t.planId() == null) return false;
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM care_task WHERE resident_id=? AND task_type_id=? AND plan_id<>? AND status IN('DUE','OVERDUE') AND window_from<=? AND window_to>=?)",
                Boolean.class,
                t.residentId(),
                t.taskTypeId(),
                t.planId(),
                local(t.plannedAt()),
                local(t.plannedAt())));
    }

    public CareTask insert(CareTask t) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var p = c.prepareStatement(
                            "INSERT INTO care_task(resident_id,location_type,location_id,task_type_id,basis_type,basis_id,plan_id,original_task_id,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,performer_id,status,data,created_by,created_at,due_at,overdue_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS);
                    Object[] args = {
                        t.residentId(),
                        t.locationType(),
                        t.locationId(),
                        t.taskTypeId(),
                        t.basisType().name(),
                        t.basisId(),
                        t.planId(),
                        t.originalTaskId(),
                        t.shiftId(),
                        t.floorAreaId(),
                        local(t.plannedAt()),
                        local(t.windowFrom()),
                        local(t.windowTo()),
                        t.importance().name(),
                        t.performerRole().name(),
                        t.performerId(),
                        t.status().name(),
                        json.writeValueAsString(t.data()),
                        t.createdBy(),
                        local(t.createdAt()),
                        t.status() != TaskStatus.NOT_YET_DUE ? local(t.createdAt()) : null,
                        t.status() == TaskStatus.OVERDUE ? local(t.createdAt()) : null
                    };
                    for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
                    return p;
                },
                keys);
        return get(keys.getKey().longValue(), false);
    }

    public CareTask transition(CareTask t, TaskStatus next, Instant at, String reason, String rule) {
        jdbc.update(
                "UPDATE care_task SET status=?,version=version+1,due_at=IF(?='DUE',?,due_at),overdue_at=IF(?='OVERDUE',?,overdue_at),closed_at=IF(? IN('CANCELLED','NOT_PERFORMED','COMPLETED','COMPLETED_LATE'),?,closed_at),closure_reason=?,closure_rule=? WHERE id=? AND version=?",
                next.name(),
                next.name(),
                local(at),
                next.name(),
                local(at),
                next.name(),
                local(at),
                reason,
                rule,
                t.id(),
                t.version());
        return get(t.id(), false);
    }

    public CareTaskClosure closure(long id) {
        return jdbc.queryForObject(
                "SELECT closure_reason,closure_rule,closed_at FROM care_task WHERE id=?",
                (rs, index) -> new CareTaskClosure(
                        rs.getString("closure_reason"), rs.getString("closure_rule"), instant(rs, "closed_at")),
                id);
    }
}
