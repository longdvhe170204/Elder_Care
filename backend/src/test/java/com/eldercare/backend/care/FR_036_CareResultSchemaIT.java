package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.support.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T035/R-15: kết quả gốc nằm trên task và bị khóa ở database. */
@Transactional
class FR_036_CareResultSchemaIT extends MySqlIntegrationTestBase {
    @Test
    void FR_036_openTaskCannotContainAPartialResultPayload() {
        long id = openTask();
        assertThatThrownBy(() -> jdbc.update("UPDATE care_task SET result_numeric_value=200 WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Autowired
    JdbcTemplate jdbc;

    long openTask() {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO care_task(id,resident_id,task_type_id,basis_type,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,status,data,created_by,created_at) VALUES(?,?,1,'AD_HOC',1,1,'2026-11-01 10:00:00','2026-11-01 09:45:00','2026-11-01 10:15:00','NORMAL','CAREGIVER','DUE','{}',1,'2026-11-01 09:00:00')",
                id,
                TestUsers.nextId());
        return id;
    }

    void close(long id) {
        jdbc.update(
                "UPDATE care_task SET status='COMPLETED',result_type='ML',result_numeric_value=200,result_performed_at='2026-11-01 10:00:00',result_recorded_at='2026-11-01 10:10:00',result_recorded_by=1,result_permission_basis='ASSIGNED',result_values='{}' WHERE id=?",
                id);
    }

    @Test
    void FR_036_resultColumnsAndResidentTypeTimeIndexExist() {
        var columns = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='care_task'",
                String.class);
        assertThat(columns)
                .contains(
                        "result_numeric_value",
                        "result_code_value",
                        "result_values",
                        "result_performed_at",
                        "result_device_at",
                        "result_recorded_at",
                        "result_recorded_by",
                        "result_late_recording",
                        "result_permission_lost",
                        "result_permission_basis",
                        "reason_type_id",
                        "awaiting_remeasurement",
                        "pending_result");
        assertThat(jdbc.queryForList(
                        "SELECT column_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='care_task' AND index_name='ix_care_task_result' ORDER BY seq_in_index",
                        String.class))
                .containsExactly("resident_id", "result_type", "result_performed_at");
    }

    @Test
    void FR_036_originalResultCannotBeChangedAfterClose() {
        long id = openTask();
        close(id);
        assertThatThrownBy(() -> jdbc.update("UPDATE care_task SET result_numeric_value=300 WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_036_closedResultCannotBeDeletedByApplication() {
        long id = openTask();
        close(id);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM care_task WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_035_performedAtCannotFollowDeviceAt() {
        long id = openTask();
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE care_task SET status='COMPLETED',result_type='ML',result_performed_at='2026-11-01 10:00:00',result_device_at='2026-11-01 09:59:59',result_recorded_at='2026-11-01 10:10:00',result_recorded_by=1,result_permission_basis='ASSIGNED',result_values='{}' WHERE id=?",
                        id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_035_performedAtCannotFollowRecordedAtWhenOnline() {
        long id = openTask();
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE care_task SET status='COMPLETED',result_type='ML',result_performed_at='2026-11-01 10:00:00',result_recorded_at='2026-11-01 09:59:59',result_recorded_by=1,result_permission_basis='ASSIGNED',result_values='{}' WHERE id=?",
                        id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_036_resultCannotBeAttachedToAnOpenTask() {
        long id = openTask();
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE care_task SET result_type='ML',result_performed_at='2026-11-01 10:00:00',result_recorded_at='2026-11-01 10:10:00',result_recorded_by=1,result_permission_basis='ASSIGNED',result_values='{}' WHERE id=?",
                        id))
                .isInstanceOf(DataAccessException.class);
    }
}
