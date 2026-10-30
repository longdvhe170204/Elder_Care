package com.eldercare.backend.care.internal.governance.persistence;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.care.internal.governance.domain.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T008: chỉ đọc bảng M04; absence của bảng story được nhận diện rõ, không nuốt lỗi SQL khác. */
@Repository
public class CareGovernanceStore {
    private final JdbcTemplate jdbc;

    public CareGovernanceStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean exists(String table) {
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?",
                        Integer.class,
                        table)
                > 0;
    }

    private boolean column(String table, String column) {
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=? AND column_name=?",
                        Integer.class,
                        table,
                        column)
                > 0;
    }

    public Target scope(String type, long id) {
        String table = switch (type) {
            case "M04.CARE_PLAN" -> "care_plan";
            case "M04.TASK" -> "care_task";
            case "M04.CARE_RESULT" -> "care_result";
            case "M04.DAY_CARE_ATTENDANCE" -> "care_day_attendance";
            default -> throw new IllegalArgumentException("Loại phạm vi không thuộc M04");
        };
        if (!exists(table)) throw missing(id);
        String floor = column(table, "floor_area_id") ? "floor_area_id" : "NULL";
        return jdbc
                .query(
                        "SELECT resident_id," + floor + " FROM " + table + " WHERE id=?",
                        (rs, i) -> new Target(
                                type, Long.toString(id), rs.getObject(1, Long.class), rs.getObject(2, Long.class)),
                        id)
                .stream()
                .findFirst()
                .filter(t -> t.residentId() != null || t.floorAreaId() != null)
                .orElseThrow(() -> missing(id));
    }

    private InvalidPermissionDataException missing(long id) {
        return new InvalidPermissionDataException("M04.SCOPE", "MISSING_RECORD", id);
    }

    public List<CareRetentionRow> rows(CareRetentionType type) {
        if (!exists(type.table())) return List.of();
        String sql = switch (type) {
            case CARE_PLAN, DAY_CARE_ATTENDANCE -> "SELECT id,resident_id,NULL planned_at FROM " + type.table();
            case CARE_PLAN_ITEM ->
                exists("care_plan")
                        ? "SELECT c.id,p.resident_id,NULL planned_at FROM care_plan_item c JOIN care_plan p ON p.id=c.care_plan_id"
                        : null;
            case RISK_AGREEMENT ->
                exists("care_plan")
                        ? "SELECT c.id,p.resident_id,NULL planned_at FROM care_risk_agreement c JOIN care_plan p ON p.id=c.care_plan_id"
                        : null;
            case RISK_OPINION ->
                exists("care_plan") && exists("care_risk_agreement")
                        ? "SELECT c.id,p.resident_id,NULL planned_at FROM care_risk_opinion c JOIN care_risk_agreement a ON a.id=c.agreement_id JOIN care_plan p ON p.id=a.care_plan_id"
                        : null;
            case TASK, LOCATION_TASK ->
                "SELECT id,resident_id,planned_at FROM care_task WHERE resident_id IS "
                        + (type.profile() ? "NOT NULL" : "NULL");
            case CARE_RESULT, LOCATION_CARE_RESULT ->
                exists("care_task")
                        ? "SELECT c.id,COALESCE(c.resident_id,t.resident_id) resident_id,t.planned_at FROM care_result c LEFT JOIN care_task t ON t.id=c.task_id WHERE COALESCE(c.resident_id,t.resident_id) IS "
                                + (type.profile() ? "NOT NULL" : "NULL")
                        : null;
            case TASK_HISTORY, LOCATION_TASK_HISTORY ->
                exists("care_task")
                        ? "SELECT c.id,t.resident_id,t.planned_at FROM care_task_history c JOIN care_task t ON t.id=c.task_id WHERE t.resident_id IS "
                                + (type.profile() ? "NOT NULL" : "NULL")
                        : null;
            case ATTENDANCE_RECORD ->
                exists("care_day_attendance")
                        ? "SELECT c.id,p.resident_id,NULL planned_at FROM care_attendance_record c JOIN care_day_attendance p ON p.id=c.attendance_id"
                        : null;
        };
        if (sql == null) return List.of();
        return jdbc.query(sql + " ORDER BY id", (rs, i) -> {
            LocalDateTime planned = rs.getObject("planned_at", LocalDateTime.class);
            return new CareRetentionRow(
                    rs.getLong("id"),
                    rs.getObject("resident_id", Long.class),
                    planned == null ? null : planned.toInstant(ZoneOffset.UTC));
        });
    }

    public Set<DataRef> references(CareRetentionType type, long id) {
        Set<DataRef> result = new LinkedHashSet<>();
        switch (type) {
            case CARE_PLAN -> {
                refs(result, "care_plan_item", "care_plan_id", id, "M04.CARE_PLAN_ITEM");
                refs(result, "care_risk_agreement", "care_plan_id", id, "M04.RISK_AGREEMENT");
                refs(result, "care_plan", "previous_plan_id", id, "M04.CARE_PLAN");
            }
            case CARE_PLAN_ITEM -> {
                refs(result, "care_plan_item", "source_item_id", id, "M04.CARE_PLAN_ITEM");
                if (exists("care_task") && column("care_task", "basis_id"))
                    result.addAll(jdbc.query(
                            "SELECT id,resident_id FROM care_task WHERE basis_type='CARE_PLAN_ITEM' AND basis_id=?",
                            (rs, i) -> new DataRef(
                                    rs.getObject(2) == null ? "M04.LOCATION_TASK" : "M04.TASK", rs.getLong(1)),
                            id));
            }
            case RISK_AGREEMENT -> {
                refs(result, "care_risk_opinion", "agreement_id", id, "M04.RISK_OPINION");
                refs(result, "care_risk_agreement", "source_agreement_id", id, "M04.RISK_AGREEMENT");
            }
            case TASK, LOCATION_TASK -> {
                refs(
                        result,
                        "care_result",
                        "task_id",
                        id,
                        type.profile() ? "M04.CARE_RESULT" : "M04.LOCATION_CARE_RESULT");
                refs(
                        result,
                        "care_task_history",
                        "task_id",
                        id,
                        type.profile() ? "M04.TASK_HISTORY" : "M04.LOCATION_TASK_HISTORY");
                if (exists("care_task") && column("care_task", "original_task_id"))
                    result.addAll(jdbc.query(
                            "SELECT id,resident_id FROM care_task WHERE original_task_id=?",
                            (rs, i) -> new DataRef(
                                    rs.getObject(2) == null ? "M04.LOCATION_TASK" : "M04.TASK", rs.getLong(1)),
                            id));
            }
            case DAY_CARE_ATTENDANCE ->
                refs(result, "care_attendance_record", "attendance_id", id, "M04.ATTENDANCE_RECORD");
            default -> {}
        }
        return Set.copyOf(result);
    }

    private void refs(Set<DataRef> result, String table, String field, long id, String dataType) {
        if (exists(table) && column(table, field))
            result.addAll(jdbc.query(
                    "SELECT id FROM " + table + " WHERE " + field + "=?",
                    (rs, i) -> new DataRef(dataType, rs.getLong(1)),
                    id));
    }

    public void delete(JdbcTemplate retention, CareRetentionType type, Collection<Long> ids) {
        if (ids.isEmpty() || !exists(type.table())) return;
        String marks = String.join(",", Collections.nCopies(ids.size(), "?"));
        if (type == CareRetentionType.RISK_AGREEMENT && exists("care_risk_participant"))
            retention.update("DELETE FROM care_risk_participant WHERE agreement_id IN (" + marks + ")", ids.toArray());
        retention.update("DELETE FROM " + type.table() + " WHERE id IN (" + marks + ")", ids.toArray());
    }
}
