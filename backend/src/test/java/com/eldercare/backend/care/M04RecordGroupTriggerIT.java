package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** T064/R-15: bảng gộp task/kết quả khai nhiều nhóm và khóa bản ghi đã chốt tại DB. */
class M04RecordGroupTriggerIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

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
