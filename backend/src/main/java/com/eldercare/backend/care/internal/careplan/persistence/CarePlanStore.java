package com.eldercare.backend.care.internal.careplan.persistence;

import com.eldercare.backend.care.api.dto.CarePlanStatus;
import com.eldercare.backend.care.internal.careplan.domain.*;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** SQL chỉ trên hai bảng của 005; bảng chung được truy cập qua API của chủ sở hữu. */
@Repository
public class CarePlanStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public CarePlanStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    private Instant instant(ResultSet rs, String field) throws SQLException {
        var value = rs.getObject(field, LocalDateTime.class);
        return value == null ? null : value.atZone(BusinessTime.ZONE).toInstant();
    }

    private LocalDateTime local(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, BusinessTime.ZONE);
    }

    private CarePlan row(ResultSet rs, int index) throws SQLException {
        return new CarePlan(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                rs.getInt("revision"),
                CarePlanStatus.valueOf(rs.getString("status")),
                rs.getObject("effective_from", LocalDate.class),
                rs.getObject("effective_to", LocalDate.class),
                rs.getObject("previous_plan_id", Long.class),
                rs.getString("change_reason"),
                rs.getLong("created_by"),
                instant(rs, "created_at"),
                rs.getObject("approved_by", Long.class),
                instant(rs, "approved_at"),
                rs.getBoolean("self_approved"),
                instant(rs, "submitted_at"),
                instant(rs, "reminded_at"),
                instant(rs, "escalated_at"),
                rs.getString("end_reason"),
                rs.getInt("version"));
    }

    public CarePlan require(long id) {
        return find(id, false);
    }

    public CarePlan locked(long id) {
        return find(id, true);
    }

    private CarePlan find(long id, boolean lock) {
        return jdbc.query("SELECT * FROM care_plan WHERE id=?" + (lock ? " FOR UPDATE" : ""), this::row, id).stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public List<CarePlan> list(long residentId, boolean lock) {
        return jdbc.query(
                "SELECT * FROM care_plan WHERE resident_id=? ORDER BY revision DESC" + (lock ? " FOR UPDATE" : ""),
                this::row,
                residentId);
    }

    public CarePlan create(long residentId, int revision, Long previous, long actor, Instant now) {
        long id = insert(
                "INSERT INTO care_plan(resident_id,revision,status,previous_plan_id,created_by,created_at) VALUES(?,?,'DRAFT',?,?,?)",
                residentId,
                revision,
                previous,
                actor,
                local(now));
        return locked(id);
    }

    private long insert(String sql, Object... values) {
        var key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
                    return statement;
                },
                key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }

    public List<CarePlanItem> items(long planId) {
        return items(planId, false);
    }

    public List<CarePlanItem> lockedItems(long planId) {
        return items(planId, true);
    }

    private List<CarePlanItem> items(long planId, boolean lock) {
        return jdbc.query(
                "SELECT id,source_item_id,task_type_id,data FROM care_plan_item WHERE care_plan_id=? AND kind='TASK' AND active ORDER BY id"
                        + (lock ? " FOR UPDATE" : ""),
                (rs, i) -> new CarePlanItem(
                        rs.getLong("id"),
                        planId,
                        rs.getObject("source_item_id", Long.class),
                        rs.getLong("task_type_id"),
                        json.readValue(rs.getString("data"), CarePlanItemData.class)),
                planId);
    }

    public void saveItems(long planId, List<CarePlanItem> items) {
        jdbc.update("UPDATE care_plan_item SET active=FALSE WHERE care_plan_id=? AND kind='TASK' AND active", planId);
        for (var item : items) {
            var data = item.data();
            if (item.id() == 0) {
                insert(
                        "INSERT INTO care_plan_item(care_plan_id,kind,source_item_id,task_type_id,frequency_type,interval_hours,starts_at,data) VALUES(?,'TASK',?,?,?,?,?,?)",
                        planId,
                        item.sourceItemId(),
                        item.physicalTaskTypeId(),
                        data.frequencyType(),
                        data.intervalHours(),
                        data.startsAt(),
                        json.writeValueAsString(data));
            } else {
                int count = jdbc.update(
                        "UPDATE care_plan_item SET active=TRUE,task_type_id=?,frequency_type=?,interval_hours=?,starts_at=?,data=? WHERE id=? AND care_plan_id=? AND kind='TASK'",
                        item.physicalTaskTypeId(),
                        data.frequencyType(),
                        data.intervalHours(),
                        data.startsAt(),
                        json.writeValueAsString(data),
                        item.id(),
                        planId);
                if (count != 1) throw new NotFoundException();
            }
        }
    }

    public void touch(long id) {
        jdbc.update("UPDATE care_plan SET version=version+1 WHERE id=?", id);
    }

    public List<LocalDate> dueDates(LocalDate today) {
        return jdbc.queryForList(
                "SELECT DISTINCT effective_from FROM care_plan WHERE status='PENDING_EFFECT' AND effective_from<=? ORDER BY effective_from",
                LocalDate.class,
                today);
    }

    public List<CarePlan> pendingOn(LocalDate date) {
        return jdbc.query(
                "SELECT * FROM care_plan WHERE status='PENDING_EFFECT' AND effective_from=? ORDER BY resident_id,id",
                this::row,
                date);
    }

    public List<CarePlan> pendingApproval() {
        return jdbc.query("SELECT * FROM care_plan WHERE status='PENDING_APPROVAL' ORDER BY resident_id,id", this::row);
    }

    public void reminder(long id, Instant remindedAt, Instant escalatedAt) {
        jdbc.update(
                "UPDATE care_plan SET reminded_at=?,escalated_at=? WHERE id=?",
                local(remindedAt),
                local(escalatedAt),
                id);
    }

    public void expire(long id, LocalDate until) {
        jdbc.update(
                "UPDATE care_plan SET status='EXPIRED',effective_to=?,end_reason='NEW_REVISION',version=version+1 WHERE id=? AND status='EFFECTIVE'",
                until,
                id);
    }

    public void activate(long id) {
        jdbc.update(
                "UPDATE care_plan SET status='EFFECTIVE',version=version+1 WHERE id=? AND status='PENDING_EFFECT'", id);
    }

    public void transition(
            CarePlan before,
            CarePlanStatus status,
            LocalDate from,
            String reason,
            Long approver,
            Instant approvedAt,
            boolean self,
            Instant submitted,
            String endReason) {
        jdbc.update(
                "UPDATE care_plan SET status=?,effective_from=?,change_reason=?,approved_by=?,approved_at=?,self_approved=?,submitted_at=?,reminded_at=NULL,escalated_at=NULL,end_reason=?,version=version+1 WHERE id=?",
                status.name(),
                from,
                reason,
                approver,
                local(approvedAt),
                self,
                local(submitted),
                endReason,
                before.id());
    }
}
