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
import java.util.ArrayList;
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

    /** Constitution XII 3.2.0, research R-15: bảng chứa nhiều nhóm có cột {@code locked_at} và hai trigger theo dòng. */
    @Test
    void XII_everyTableWithLockedAt_hasRowLockAndNoDeleteTriggers() throws SQLException {
        List<String> tables = new ArrayList<>();
        Set<String> triggers = new HashSet<>();
        try (Connection c = adminConnection();
                Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT LOWER(TABLE_NAME) FROM information_schema.COLUMNS"
                    // shedlock là bảng khóa job của thư viện ShedLock, cột locked_at không phải khóa theo dòng
                    + " WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'locked_at' AND TABLE_NAME <> 'shedlock'")) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
            try (ResultSet rs = st.executeQuery(
                    "SELECT LOWER(TRIGGER_NAME) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE()")) {
                while (rs.next()) {
                    triggers.add(rs.getString(1));
                }
            }
        }

        // Entity @DataGroup(MULTIPLE) phải nằm ở bảng có locked_at (R-05)
        List<String> multipleGroupTables = new ClassFileImporter()
                .importPackages("com.eldercare.backend").stream()
                        .filter(c -> c.isAnnotatedWith(Entity.class) && c.isAnnotatedWith(DataGroup.class))
                        .filter(c -> c.getAnnotationOfType(DataGroup.class).value() == DataGroup.Group.MULTIPLE)
                        .map(c -> c.getAnnotationOfType(Table.class).name())
                        .toList();
        assertThat(tables).contains("simple_record").containsAll(multipleGroupTables);
        assertThat(tables)
                .allSatisfy(table ->
                        assertThat(triggers).contains("trg_" + table + "_row_lock", "trg_" + table + "_no_delete"));
    }

    /**
     * Audit cơ sở dữ liệu F-04: nguồn nhóm dữ liệu không phụ thuộc {@code @Entity}. Mỗi bảng khai nhóm bằng
     * {@code @Entity} + {@code @DataGroup} hoặc bằng COMMENT của bảng ({@code DataGroup=CATALOG|BUSINESS|RECORD|MULTIPLE|
     * TECHNICAL}); bảng nhóm 3 khai bằng COMMENT có hai trigger chặn sửa, xóa. Bảng có sẵn chưa khai ở
     * {@link #PENDING_DATA_GROUP}, chủ module khai khi làm migration gộp bảng {@code _95} (research R-15).
     */
    @Test
    void F_04_everyTable_declaresDataGroup_andRecordTablesHaveTriggers() throws SQLException {
        java.util.Map<String, String> entityGroups = new java.util.HashMap<>();
        new ClassFileImporter()
                .importPackages("com.eldercare.backend").stream()
                        .filter(c -> c.isAnnotatedWith(Entity.class) && c.isAnnotatedWith(DataGroup.class))
                        .forEach(c -> entityGroups.put(
                                c.getAnnotationOfType(Table.class).name().toLowerCase(),
                                c.getAnnotationOfType(DataGroup.class).value().name()));
        java.util.Map<String, String> commentGroups = new java.util.HashMap<>();
        List<String> tables = new ArrayList<>();
        Set<String> triggers = new HashSet<>();
        java.util.regex.Pattern group = java.util.regex.Pattern.compile("DataGroup=([A-Z]+)");
        try (Connection c = adminConnection();
                Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT LOWER(TABLE_NAME), TABLE_COMMENT FROM information_schema.TABLES"
                    + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'"
                    + " AND TABLE_NAME <> 'flyway_schema_history'")) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                    java.util.regex.Matcher m = group.matcher(rs.getString(2));
                    if (m.find()) {
                        commentGroups.put(rs.getString(1), m.group(1));
                    }
                }
            }
            try (ResultSet rs = st.executeQuery(
                    "SELECT LOWER(TRIGGER_NAME) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE()")) {
                while (rs.next()) {
                    triggers.add(rs.getString(1));
                }
            }
        }

        List<String> problems = new ArrayList<>();
        for (String table : tables) {
            String byEntity = entityGroups.get(table);
            String byComment = commentGroups.get(table);
            if (byEntity == null && byComment == null && !PENDING_DATA_GROUP.contains(table)) {
                problems.add(table + ": chưa khai nhóm (COMMENT DataGroup=… hoặc @Entity @DataGroup)");
            }
            if (byEntity != null && byComment != null && !byEntity.equals(byComment)) {
                problems.add(table + ": @DataGroup " + byEntity + " khác COMMENT " + byComment);
            }
            if ("RECORD".equals(byComment)
                    && !triggers.containsAll(List.of("trg_" + table + "_no_update", "trg_" + table + "_no_delete"))) {
                problems.add(table + ": nhóm 3 thiếu trigger no_update, no_delete");
            }
            if (PENDING_DATA_GROUP.contains(table) && (byEntity != null || byComment != null)) {
                problems.add(table + ": đã khai nhóm, xóa khỏi PENDING_DATA_GROUP");
            }
        }
        assertThat(problems).isEmpty();
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

    @Test
    void DBR_23_m03SqlRecordTablesHaveBothAppendOnlyTriggers() throws SQLException {
        // room_isolation_history là bảng SQL thuần nên phép quét @Entity phía trên không thấy.
        try (Connection c = adminConnection();
                var query = c.prepareStatement(
                        "SELECT EVENT_MANIPULATION,ACTION_TIMING,ACTION_STATEMENT FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND EVENT_OBJECT_TABLE=?")) {
            for (String table : List.of("bed_state_history", "room_isolation_history")) {
                query.setString(1, table);
                Set<String> events = new HashSet<>();
                try (var rows = query.executeQuery()) {
                    while (rows.next()) {
                        events.add(rows.getString(1));
                        assertThat(rows.getString(2)).isEqualTo("BEFORE");
                        assertThat(rows.getString(3)).contains("45000");
                    }
                }
                assertThat(events).as(table).contains("UPDATE", "DELETE");
            }
        }
    }

    /** Bảng có sẵn chưa khai nhóm (rà 2026-10-30); chủ module khai khi làm migration gộp bảng, xong thì xóa dòng. */
    private static final Set<String> PENDING_DATA_GROUP = Set.of(
            // B (001)
            "assessment_care_need",
            "care_level",
            "care_level_history",
            "consent",
            "consent_family_member",
            "end_of_life_wish",
            "end_of_life_wish_opinion",
            "file",
            "periodic_reminder",
            "reassessment_notice",
            "return_overdue_alert",
            "treatment_wish",
            "treatment_wish_notify",
            "treatment_wish_opinion",
            // C (003, T066)
            "allocation_write_guard",
            "bed_backfill_request",
            "bed_state_history",
            "day_care_capacity_guard",
            "room_allowed_care_level",
            "room_isolation_history",
            "room_type_care_level",
            // D (006, T069)
            "m07_job_run",
            "medication_active_ingredient",
            "prescription_active_ingredient",
            "resident_medication",
            // E (008, T050)
            "credential",
            "credential_practice_scope",
            "credential_type",
            "department",
            "department_lead_history",
            "floor_manager_assignment",
            "floor_scope_lock",
            "handover_assessment",
            "handover_comment",
            "handover_item",
            "handover_reply",
            "m09_job_run",
            "practice_scope",
            "shift_handover",
            "shift_lead_history",
            "shift_publication",
            "shift_schedule",
            "shift_staff",
            "shift_template",
            "staff_assignment",
            "staff_department",
            "staff_import_batch",
            "staff_profile",
            "staffing_ratio_calculation",
            "work_shift");
}
