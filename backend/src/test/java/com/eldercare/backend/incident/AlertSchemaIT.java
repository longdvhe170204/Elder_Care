package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * T015: Kiểm tra schema bảng alert, ràng buộc khóa mở duy nhất (DBR-18) và trigger bảo vệ.
 */
class AlertSchemaIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void DBR_18_openLockUniquePerResidentAndTriggerGuards() throws Exception {
        long residentId = TestUsers.nextId();
        String lockKey = "MONITORING_ALERT:SPO2";

        // Chèn cảnh báo 1 ở trạng thái mở
        jdbc.update("""
                INSERT INTO alert (resident_id, type, type_lock, level, first_at, latest_at, status, version)
                VALUES (?, 'CHI_SO', ?, 'MEDIUM', NOW(6), NOW(6), 'NEW', 0)
                """, residentId, lockKey);

        // Chèn cảnh báo 2 trùng residentId và lockKey khi cảnh báo 1 đang mở -> vi phạm UNIQUE uq_alert_open_lock
        assertThatThrownBy(() -> jdbc.update("""
                        INSERT INTO alert (resident_id, type, type_lock, level, first_at, latest_at, status, version)
                        VALUES (?, 'CHI_SO', ?, 'CRITICAL', NOW(6), NOW(6), 'NEW', 0)
                        """, residentId, lockKey)).isInstanceOf(Exception.class);

        // Đóng cảnh báo 1
        jdbc.update("""
                UPDATE alert SET status = 'CLOSED', closed_at = NOW(6), resolution = 'DA_XU_LY', version = version + 1
                WHERE resident_id = ? AND type_lock = ?
                """, residentId, lockKey);

        // Sau khi đóng, có thể chèn cảnh báo mới cùng khóa
        int inserted = jdbc.update("""
                INSERT INTO alert (resident_id, type, type_lock, level, first_at, latest_at, status, version)
                VALUES (?, 'CHI_SO', ?, 'LOW', NOW(6), NOW(6), 'NEW', 0)
                """, residentId, lockKey);
        assertThat(inserted).isEqualTo(1);

        // Kiểm tra trigger: chặn xóa ngoại trừ retention user
        try (Connection admin = adminConnection()) {
            assertThatThrownBy(() -> execute(admin, "DELETE FROM alert WHERE resident_id=" + residentId))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("retention");
        } finally {
            try (Connection retention = retentionConnection()) {
                execute(retention, "DELETE FROM alert WHERE resident_id=" + residentId);
            }
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
