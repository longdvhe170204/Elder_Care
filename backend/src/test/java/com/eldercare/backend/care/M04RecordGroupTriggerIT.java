package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.SQLException;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** T064/R-15: bảng gộp task/kết quả khai nhiều nhóm và khóa bản ghi đã chốt tại DB. */
class M04RecordGroupTriggerIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void XII_005ConsolidatedTablesDeclareGroupsAndRemoveLegacyTables() {
        assertThat(jdbc.queryForObject(
                        "SELECT table_comment FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='care_task'",
                        String.class))
                .isEqualTo("DataGroup=MULTIPLE");
        assertThat(jdbc.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('care_task_type','care_task_type_certificate','care_result_value','care_non_performance_reason_type','care_resident_lock','m04_job_milestone','care_result','care_attendance')",
                        String.class))
                .isEmpty();
        assertThat(jdbc.queryForList(
                        "SELECT table_comment FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('care_plan','care_plan_item')",
                        String.class))
                .containsExactlyInAnyOrder("DataGroup=BUSINESS", "DataGroup=BUSINESS");
    }

    @Test
    void XII_005MixedTableHasLockColumnAndBothTriggers() throws SQLException {
        assertThat(jdbc.queryForList(
                        "SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='care_task'",
                        String.class))
                .contains("locked_at");
        var triggers = new HashSet<String>();
        try (var c = adminConnection();
                var s = c.createStatement();
                var r = s.executeQuery(
                        "SELECT trigger_name FROM information_schema.triggers WHERE trigger_schema=DATABASE() AND event_object_table='care_task'")) {
            while (r.next()) triggers.add(r.getString(1));
        }
        assertThat(triggers).contains("trg_care_task_row_lock", "trg_care_task_no_delete");
    }

    @Test
    void FR_036_closedRowsCannotBeChangedOrDeletedEvenByAdmin() throws SQLException {
        try (var c = adminConnection();
                var s = c.createStatement()) {
            c.setAutoCommit(false);
            try {
                long id = TestUsers.nextId();
                s.executeUpdate(
                        "INSERT INTO care_task(id,resident_id,task_type_id,basis_type,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,status,data,created_by,created_at) VALUES("
                                + id
                                + ",1,1,'AD_HOC',1,1,'2026-11-01 10:00:00','2026-11-01 09:45:00','2026-11-01 10:15:00','NORMAL','CAREGIVER','DUE','{}',1,'2026-11-01 09:00:00')");
                s.executeUpdate(
                        "UPDATE care_task SET status='CANCELLED',closed_at='2026-11-01 10:00:00' WHERE id=" + id);
                assertThatThrownBy(() -> s.executeUpdate("UPDATE care_task SET data='{}' WHERE id=" + id))
                        .isInstanceOf(SQLException.class);
                assertThatThrownBy(() -> s.executeUpdate("DELETE FROM care_task WHERE id=" + id))
                        .isInstanceOf(SQLException.class);
            } finally {
                c.rollback();
            }
        }
    }
}
