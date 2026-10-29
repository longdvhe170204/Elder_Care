package com.eldercare.backend.administration.auditlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * FR-048, FR-051, DBR-23 (000 T026): bảng audit_log phân vùng theo năm, chỉ thêm; chỉ eldercare_retention xóa được,
 * và không xóa được bản tóm tắt đợt loại bỏ.
 */
class AuditLogTableIT extends MySqlIntegrationTestBase {

    private static final String INSERT =
            "INSERT INTO audit_log (occurred_at, actor_type, actor_id, role, action, entity_type, entity_id,"
                    + " exempt_from_purge) VALUES ('2026-10-19 03:00:00', 'USER', 1, 'AC-01', 'TEST', 'X', ?, ?)";

    @Autowired
    JdbcTemplate jdbc;

    private long insert(String entityId, boolean exempt) {
        jdbc.update(INSERT, entityId, exempt);
        return jdbc.queryForObject("SELECT id FROM audit_log WHERE entity_id = ?", Long.class, entityId);
    }

    @Test
    void R_06_table_isPartitionedByYear() {
        Integer partitions = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.PARTITIONS"
                        + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'audit_log'",
                Integer.class);

        assertThat(partitions).isGreaterThan(1);
    }

    @Test
    void FR_048_appAccount_cannotUpdateOrDelete() {
        insert("app-1", false);

        assertThatThrownBy(() -> jdbc.update("UPDATE audit_log SET action = 'X' WHERE entity_id = 'app-1'"))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("UPDATE command denied");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM audit_log WHERE entity_id = 'app-1'"))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("DELETE command denied");
    }

    @Test
    void FR_048_triggers_blockEvenAdmin() throws SQLException {
        insert("admin-1", false);

        try (Connection c = adminConnection();
                Statement s = c.createStatement()) {
            assertThatThrownBy(() -> s.executeUpdate("UPDATE audit_log SET action = 'X' WHERE entity_id = 'admin-1'"))
                    .isInstanceOf(SQLException.class)
                    .hasFieldOrPropertyWithValue("SQLState", "45000");
            assertThatThrownBy(() -> s.executeUpdate("DELETE FROM audit_log WHERE entity_id = 'admin-1'"))
                    .isInstanceOf(SQLException.class)
                    .hasFieldOrPropertyWithValue("SQLState", "45000");
        }
    }

    @Test
    void FR_051_retentionAccount_deletesOnlyNonExemptRows() throws SQLException {
        insert("ret-normal", false);
        insert("ret-summary", true);

        try (Connection c = retentionConnection();
                Statement s = c.createStatement()) {
            assertThat(s.executeUpdate("DELETE FROM audit_log WHERE entity_id = 'ret-normal'"))
                    .isEqualTo(1);
            assertThatThrownBy(() -> s.executeUpdate("DELETE FROM audit_log WHERE entity_id = 'ret-summary'"))
                    .isInstanceOf(SQLException.class)
                    .hasFieldOrPropertyWithValue("SQLState", "45000");
        }
    }
}
