package com.eldercare.backend.care.internal.governance.persistence;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.care.internal.attendance.persistence.AttendanceStore;
import com.eldercare.backend.care.internal.governance.domain.*;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T008: chỉ đọc bảng M04; absence của bảng story được nhận diện rõ, không nuốt lỗi SQL khác. */
@Repository
public class CareGovernanceStore {
    private final JdbcTemplate jdbc;
    private final AttendanceStore attendance;

    public CareGovernanceStore(JdbcTemplate jdbc, AttendanceStore attendance) {
        this.jdbc = jdbc;
        this.attendance = attendance;
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
        var activity = activityScope(type, id);
        if (activity.isPresent()) return activity.get();
        if (type.equals("M04.DAY_CARE_ATTENDANCE")) {
            try {
                var row = attendance.require(id);
                return new Target(
                        type, Long.toString(id), row.residentId(), row.content().floorAreaId());
            } catch (com.eldercare.backend.common.error.NotFoundException e) {
                throw missing(id);
            }
        }
        String table = switch (type) {
            case "M04.CARE_PLAN" -> "care_plan";
            case "M04.TASK" -> "care_task";
            case "M04.CARE_RESULT" -> "care_task";
            case "M04.DAY_CARE_ATTENDANCE" -> "care_day_attendance";
            default -> throw new IllegalArgumentException("Loại phạm vi không thuộc M04");
        };
        if (!exists(table)) throw missing(id);
        String floor = column(table, "floor_area_id") ? "floor_area_id" : "NULL";
        return jdbc
                .query(
                        "SELECT resident_id," + floor + " FROM " + table + " WHERE id=?"
                                + (type.equals("M04.CARE_RESULT") ? " AND result_performed_at IS NOT NULL" : ""),
                        (rs, i) -> new Target(
                                type, Long.toString(id), rs.getObject(1, Long.class), rs.getObject(2, Long.class)),
                        id)
                .stream()
                .findFirst()
                .filter(t -> t.residentId() != null || t.floorAreaId() != null)
                .orElseThrow(() -> missing(id));
    }

    /**
     * 014 T006 (module-interfaces 1.1): buổi, chuyến (chung bảng {@code activity_session}, data-model mục 0) theo tầng;
     * đăng ký, sở thích theo người cao tuổi (tầng lấy từ vị trí hiện tại); danh sách kiểm tra theo tầng của danh sách.
     * Hai bảng riêng tạo ở US1, trước đó mọi id là thiếu bản ghi.
     */
    private Optional<Target> activityScope(String type, long id) {
        String sql = switch (type) {
            case "M04.ACTIVITY_SESSION", "M04.TRIP" ->
                exists("activity_session") ? "SELECT NULL, floor_area_id FROM activity_session WHERE id=?" : "";
            case "M04.ACTIVITY_REGISTRATION" ->
                exists("activity_registration") ? "SELECT resident_id, NULL FROM activity_registration WHERE id=?" : "";
            case "M04.PREFERENCE" -> "SELECT resident_id, NULL FROM simple_record WHERE kind='M04.PREFERENCE' AND id=?";
            case "M04.QUALITY_CHECK" ->
                "SELECT NULL, CAST(JSON_UNQUOTE(JSON_EXTRACT(content,'$.floorAreaId')) AS UNSIGNED) FROM simple_record"
                        + " WHERE kind='M04.QUALITY_CHECKLIST' AND id=?";
            default -> null;
        };
        if (sql == null) return Optional.empty();
        if (sql.isEmpty()) throw missing(id);
        return Optional.of(jdbc
                .query(
                        sql,
                        (rs, i) -> new Target(
                                type, Long.toString(id), rs.getObject(1, Long.class), rs.getObject(2, Long.class)),
                        id)
                .stream()
                .findFirst()
                .filter(t -> t.residentId() != null || t.floorAreaId() != null)
                .orElseThrow(() -> missing(id)));
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
                        ? "SELECT id,resident_id,planned_at FROM care_task WHERE result_performed_at IS NOT NULL AND resident_id IS "
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
            case ACTIVITY_SESSION -> "SELECT id,NULL resident_id,starts_at planned_at FROM activity_session";
            case ACTIVITY_REGISTRATION -> "SELECT id,resident_id,NULL planned_at FROM activity_registration";
        };
        if (sql == null) return List.of();
        return jdbc.query(sql + " ORDER BY id", (rs, i) -> {
            LocalDateTime planned = rs.getObject("planned_at", LocalDateTime.class);
            return new CareRetentionRow(
                    rs.getLong("id"),
                    rs.getObject("resident_id", Long.class),
                    planned == null ? null : planned.atZone(BusinessTime.ZONE).toInstant());
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
                if (exists("care_task")
                        && column("care_task", "result_performed_at")
                        && Boolean.TRUE.equals(jdbc.queryForObject(
                                "SELECT EXISTS(SELECT 1 FROM care_task WHERE id=? AND result_performed_at IS NOT NULL)",
                                Boolean.class,
                                id)))
                    result.add(new DataRef(type.profile() ? "M04.CARE_RESULT" : "M04.LOCATION_CARE_RESULT", id));
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
            case ACTIVITY_SESSION ->
                refs(result, "activity_registration", "session_id", id, "M04.ACTIVITY_REGISTRATION");
            case CARE_RESULT, LOCATION_CARE_RESULT ->
                result.add(new DataRef(type.profile() ? "M04.TASK" : "M04.LOCATION_TASK", id));
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
        // R-15: cả hai định danh có trong graph để kiểm attachment; chỉ TASK xóa dòng vật lý.
        if (type == CareRetentionType.CARE_RESULT || type == CareRetentionType.LOCATION_CARE_RESULT) return;
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
