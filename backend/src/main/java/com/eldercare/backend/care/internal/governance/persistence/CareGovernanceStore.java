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
        if (type == CareRetentionType.RISK_OPINION) return List.of();
        if (!exists(type.table())) return List.of();
        String sql = switch (type) {
            case CARE_PLAN, DAY_CARE_ATTENDANCE -> "SELECT id,resident_id,NULL planned_at FROM " + type.table();
            case CARE_PLAN_ITEM ->
                exists("care_plan")
                        ? "SELECT c.id,p.resident_id,NULL planned_at FROM care_plan_item c JOIN care_plan p ON p.id=c.care_plan_id WHERE c.kind='TASK'"
                        : null;
            case RISK_AGREEMENT ->
                exists("care_plan")
                        ? "SELECT c.id,p.resident_id,NULL planned_at FROM care_plan_item c JOIN care_plan p ON p.id=c.care_plan_id WHERE c.kind='RISK_AGREEMENT'"
                        : null;
            case RISK_OPINION -> null;
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
                itemRefs(result, "care_plan_id", id);
                refs(result, "care_plan", "previous_plan_id", id, "M04.CARE_PLAN");
            }
            case CARE_PLAN_ITEM -> {
                itemRefs(result, "source_item_id", id);
                if (exists("care_task") && column("care_task", "basis_id"))
                    result.addAll(jdbc.query(
                            "SELECT id,resident_id FROM care_task WHERE basis_type='CARE_PLAN_ITEM' AND basis_id=?",
                            (rs, i) -> new DataRef(
                                    rs.getObject(2) == null ? "M04.LOCATION_TASK" : "M04.TASK", rs.getLong(1)),
                            id));
            }
            case RISK_AGREEMENT -> {
                // Con bảng chung bị 000 xóa trước cha qua SharedRowRetention.deleteFor(EntityRef).
                itemRefs(result, "source_item_id", id);
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

    private void itemRefs(Set<DataRef> result, String field, long id) {
        if (exists("care_plan_item"))
            result.addAll(jdbc.query(
                    "SELECT id,kind FROM care_plan_item WHERE " + field + "=?",
                    (rs, i) -> new DataRef(
                            rs.getString("kind").equals("TASK") ? "M04.CARE_PLAN_ITEM" : "M04.RISK_AGREEMENT",
                            rs.getLong("id")),
                    id));
    }

    public void delete(JdbcTemplate retention, CareRetentionType type, Collection<Long> ids) {
        if (type == CareRetentionType.RISK_OPINION) {
            if (!ids.isEmpty())
                throw new IllegalArgumentException("Ý kiến là con bảng chung, chỉ 000 xóa cùng thỏa thuận");
            return;
        }
        if (ids.isEmpty() || !exists(type.table())) return;
        String marks = String.join(",", Collections.nCopies(ids.size(), "?"));
        String kind = switch (type) {
            case CARE_PLAN_ITEM -> " AND kind='TASK'";
            case RISK_AGREEMENT -> " AND kind='RISK_AGREEMENT'";
            default -> "";
        };
        retention.update("DELETE FROM " + type.table() + " WHERE id IN (" + marks + ")" + kind, ids.toArray());
    }
}
