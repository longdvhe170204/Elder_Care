package com.cambae.backend.m15;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.data.DataGroup;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * US1 #7, SC-002, FR-009, FR-010, BR-M15-03 (000 T043; quickstart K15): mọi bảng của entity nhóm 3 có hai trigger chặn
 * sửa, xóa; tài khoản ứng dụng bị MySQL chặn khi sửa, xóa trực tiếp.
 */
class RecordGroupTriggerIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_010_everyRecordGroupTable_hasBothTriggers() throws SQLException {
        List<String> tables = new ClassFileImporter()
                .importPackages("com.cambae.backend").stream()
                        .filter(c -> c.isAnnotatedWith(Entity.class) && c.isAnnotatedWith(DataGroup.class))
                        .filter(c -> c.getAnnotationOfType(DataGroup.class).value() == DataGroup.Group.RECORD)
                        .map(c -> c.getAnnotationOfType(Table.class).name())
                        .toList();
        // Tài khoản ứng dụng không có quyền TRIGGER nên information_schema ẩn trigger với nó; đọc bằng quản trị
        Set<String> triggers = new HashSet<>();
        try (Connection c = adminConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT LOWER(TRIGGER_NAME) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE()")) {
            while (rs.next()) {
                triggers.add(rs.getString(1));
            }
        }

        assertThat(tables).isNotEmpty();
        assertThat(tables)
                .allSatisfy(table ->
                        assertThat(triggers).contains("trg_" + table + "_no_update", "trg_" + table + "_no_delete"));
    }

    @Test
    void SC_002_appAccount_cannotUpdateOrDeleteRecords() {
        jdbc.update(
                "INSERT INTO sample_office_record (occurred_at, actor_id, note) VALUES (UTC_TIMESTAMP(6), 1, 'k15')");

        assertThatThrownBy(() -> jdbc.update("UPDATE sample_office_record SET note = 'sửa'"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM sample_office_record"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void SC_002_evenAdmin_isBlockedByTriggers() throws SQLException {
        try (Connection c = adminConnection();
                Statement s = c.createStatement()) {
            s.executeUpdate(
                    "INSERT INTO sample_office_record (occurred_at, actor_id, note) VALUES (UTC_TIMESTAMP(6), 1, 'admin')");

            assertThatThrownBy(() -> s.executeUpdate("UPDATE sample_office_record SET note = 'sửa'"))
                    .isInstanceOf(SQLException.class)
                    .hasFieldOrPropertyWithValue("SQLState", "45000");
            assertThatThrownBy(() -> s.executeUpdate("DELETE FROM sample_office_record"))
                    .isInstanceOf(SQLException.class)
                    .hasFieldOrPropertyWithValue("SQLState", "45000");
        }
    }
}
