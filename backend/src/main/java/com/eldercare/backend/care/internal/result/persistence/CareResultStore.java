package com.eldercare.backend.care.internal.result.persistence;

import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.internal.result.domain.*;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Chỉ SQL care_task; đóng và thêm kết quả trong cùng UPDATE dưới khóa dòng. */
@Repository
public class CareResultStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public CareResultStore(JdbcTemplate jdbc, JsonMapper json) {
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

    @SuppressWarnings("unchecked")
    private CareResult row(ResultSet rs, int index) throws SQLException {
        return new CareResult(
                rs.getLong("id"),
                rs.getObject("resident_id", Long.class),
                rs.getString("result_type"),
                json.readValue(rs.getString("result_values"), Map.class),
                instant(rs, "result_performed_at"),
                instant(rs, "result_recorded_at"),
                instant(rs, "result_device_at"),
                rs.getLong("result_recorded_by"),
                rs.getBoolean("result_late_recording"),
                rs.getBoolean("result_permission_lost"),
                rs.getString("result_permission_basis"),
                rs.getObject("vital_sign_record_id", Long.class));
    }

    public Optional<CareResult> find(long taskId) {
        return jdbc
                .query("SELECT * FROM care_task WHERE id=? AND result_performed_at IS NOT NULL", this::row, taskId)
                .stream()
                .findFirst();
    }

    public Long closedBy(long taskId) {
        return jdbc.queryForObject("SELECT closed_by_id FROM care_task WHERE id=?", Long.class, taskId);
    }

    public Optional<PendingCareResult> pending(long taskId) {
        return jdbc
                .query(
                        "SELECT pending_result FROM care_task WHERE id=? AND awaiting_remeasurement=TRUE",
                        (rs, i) -> json.readValue(rs.getString(1), PendingCareResult.class),
                        taskId)
                .stream()
                .findFirst();
    }

    public void await(CareTask task, PendingCareResult pending) {
        changed(jdbc.update(
                "UPDATE care_task SET awaiting_remeasurement=TRUE,pending_result=?,version=version+1 WHERE id=? AND version=? AND status IN('DUE','OVERDUE') AND NOT awaiting_remeasurement",
                json.writeValueAsString(pending),
                task.id(),
                task.version()));
    }

    public List<CareResult> history(long resident, Instant from, Instant to) {
        return jdbc.query(
                "SELECT * FROM care_task WHERE resident_id=? AND result_performed_at IS NOT NULL AND (? IS NULL OR result_performed_at>=?) AND (? IS NULL OR result_performed_at<?) ORDER BY result_performed_at,id",
                this::row,
                resident,
                local(from),
                local(from),
                local(to),
                local(to));
    }

    public void complete(
            CareTask task,
            CareResultInput input,
            TaskStatus next,
            Instant performedAt,
            Instant recordedAt,
            Instant deviceAt,
            long accountId,
            String basis,
            boolean late,
            boolean permissionLost,
            Long vitalSignId) {
        int changed = jdbc.update(
                "UPDATE care_task SET status=?,version=version+1,closed_at=?,closure_reason=?,closure_rule='FR_033_RECORDED',closed_by_id=?,closed_by_type='STAFF',result_type=?,result_numeric_value=?,result_code_value=?,result_values=?,result_performed_at=?,result_recorded_at=?,result_device_at=?,result_recorded_by=?,result_late_recording=?,result_permission_lost=?,result_permission_basis=?,vital_sign_record_id=?,meal_id=?,awaiting_remeasurement=FALSE,pending_result=NULL WHERE id=? AND version=? AND result_performed_at IS NULL AND status IN('DUE','OVERDUE','COMPLETED')",
                next.name(),
                local(recordedAt),
                input.reason() == null ? null : input.reason().value(),
                accountId,
                task.data().resultType(),
                input.numericValue(),
                input.codeValue(),
                json.writeValueAsString(input.values()),
                local(performedAt),
                local(recordedAt),
                local(deviceAt),
                accountId,
                late,
                permissionLost,
                basis,
                vitalSignId,
                task.basisType().name().equals("MEAL") ? task.basisId() : null,
                task.id(),
                task.version());
        changed(changed);
    }

    public void notPerformed(CareTask task, long reasonTypeId, String note, Instant at, long accountId) {
        changed(jdbc.update(
                "UPDATE care_task SET status='NOT_PERFORMED',version=version+1,closed_at=?,closure_reason=?,closure_rule='FR_028A_NOT_PERFORMED',closed_by_id=?,closed_by_type='STAFF',reason_type_id=?,awaiting_remeasurement=FALSE,pending_result=NULL WHERE id=? AND version=? AND status IN('DUE','OVERDUE')",
                local(at),
                note,
                accountId,
                reasonTypeId,
                task.id(),
                task.version()));
    }

    private void changed(int count) {
        if (count != 1)
            throw new BusinessException(HttpStatus.CONFLICT, "CARE_TASK_CLOSED", "Công việc đã đóng hoặc đã thay đổi");
    }
}
