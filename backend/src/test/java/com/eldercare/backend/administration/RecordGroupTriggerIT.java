package com.eldercare.backend.administration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.data.DataGroup;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
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
                .importPackages("com.eldercare.backend").stream()
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

    /** Nhóm được khai bằng COMMENT của bảng (research R-15; F-04). */
    private static final Set<String> COMMENT_GROUPS = Set.of("CATALOG", "BUSINESS", "RECORD", "MULTIPLE", "TECHNICAL");

    /** Bảng có sẵn chưa khai nhóm (rà 2026-10-30); chủ module khai khi làm migration gộp bảng, xong thì xóa dòng. */
    private static final Set<String> PENDING_DATA_GROUP = Set.of(
            // C (003, T066)
            "allocation_write_guard",
            "bed_backfill_request",
            "bed_state_history",
            "day_care_capacity_guard",
            "room_allowed_care_level",
            "room_isolation_history",
            "room_type_care_level");
}
