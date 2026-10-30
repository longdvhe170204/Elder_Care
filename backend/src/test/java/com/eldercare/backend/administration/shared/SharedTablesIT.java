package com.eldercare.backend.administration.shared;

import static com.eldercare.backend.administration.shared.SharedTestKinds.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Ràng buộc ở cơ sở dữ liệu của bảy bảng dùng chung (000 T108, research R-15, data-model 000 mục 8, constitution XII
 * 3.2.0): khóa theo dòng chặn sửa, xóa dòng đã khóa kể cả tài khoản quản trị; {@code ends_at} đặt một lần;
 * {@code open_key}, {@code unique_key}, {@code active_name} theo loại; tài khoản ứng dụng không xóa bản ghi nhóm 3.
 */
class SharedTablesIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    private long simpleRecord(boolean locked) {
        jdbc.update(
                "INSERT INTO simple_record (kind, module, status, content, created_at, created_by, version, locked_at)"
                        + " VALUES ('M15.TEST_RECORD', 'M15', 'OPEN', '{}', UTC_TIMESTAMP(6), 1, 0, "
                        + (locked ? "UTC_TIMESTAMP(6)" : "NULL") + ")");
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void XII_lockedSimpleRecord_cannotBeUpdatedOrDeleted_evenByAdmin() throws SQLException {
        long open = simpleRecord(false);
        long locked = simpleRecord(true);

        jdbc.update("UPDATE simple_record SET status = 'CONFIRMED', locked_at = UTC_TIMESTAMP(6) WHERE id = ?", open);
        assertThatThrownBy(() -> jdbc.update("UPDATE simple_record SET status = 'X' WHERE id = ?", open))
                .isInstanceOf(DataAccessException.class);
        try (Connection c = adminConnection();
                Statement s = c.createStatement()) {
            assertThatThrownBy(() -> s.executeUpdate("UPDATE simple_record SET status = 'X' WHERE id = " + locked))
                    .isInstanceOf(SQLException.class)
                    .hasFieldOrPropertyWithValue("SQLState", "45000");
            assertThatThrownBy(() -> s.executeUpdate("DELETE FROM simple_record WHERE id = " + locked))
                    .isInstanceOf(SQLException.class)
                    .hasFieldOrPropertyWithValue("SQLState", "45000");
        }
        try (Connection c = retentionConnection();
                Statement s = c.createStatement()) {
            assertThat(s.executeUpdate("DELETE FROM simple_record WHERE id = " + locked))
                    .isEqualTo(1);
        }
    }

    @Test
    void statusHistory_endsAtIsSetOnce_otherColumnsNeverChange() {
        long entity = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at,"
                        + " by_system) VALUES ('M15.TEST_ENTITY', ?, 'status', 'M15', 'PERIOD', 'A', UTC_TIMESTAMP(6), FALSE)",
                entity);
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // Khoảng thứ hai khi khoảng đầu còn mở: trùng open_key
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO status_history (entity_type, entity_id, attribute, module,"
                                + " kind, value, starts_at, by_system) VALUES ('M15.TEST_ENTITY', ?, 'status', 'M15', 'PERIOD',"
                                + " 'B', UTC_TIMESTAMP(6), FALSE)",
                        entity))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE status_history SET value = 'X' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        jdbc.update("UPDATE status_history SET ends_at = UTC_TIMESTAMP(6) WHERE id = ?", id);
        assertThatThrownBy(() -> jdbc.update("UPDATE status_history SET ends_at = UTC_TIMESTAMP(6) WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM status_history WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void recordEvent_confirmedRowIsLocked_draftIsEditable() {
        jdbc.update("INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system, occurred_at)"
                + " VALUES ('M15.TEST_ENTITY', 1, 'M15', 'M15.TEST_EVENT', '{}', FALSE, UTC_TIMESTAMP(6))");
        long draft = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        jdbc.update(
                "UPDATE record_event SET content = '{\"a\":1}', confirmed_at = UTC_TIMESTAMP(6) WHERE id = ?", draft);
        assertThatThrownBy(() -> jdbc.update("UPDATE record_event SET content = '{}' WHERE id = ?", draft))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM record_event WHERE id = ?", draft))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void catalog_activeNameAndCodeAreUniquePerKindAndParent() {
        String name = unique("Tên");
        String insert = "INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, version)"
                + " VALUES (?, 'M15', ?, ?, 0, ?, 0)";
        jdbc.update(insert, "M15.TEST_CATALOG", null, name, "ACTIVE");

        assertThatThrownBy(() -> jdbc.update(insert, "M15.TEST_CATALOG", null, name, "ACTIVE"))
                .isInstanceOf(DataAccessException.class);
        jdbc.update(insert, "M15.TEST_CATALOG", null, name, "INACTIVE");
        jdbc.update(insert, "M15.TEST_OTHER_CATALOG", null, name, "ACTIVE");

        String code = unique("C");
        jdbc.update(insert, "M15.TEST_CATALOG", code, unique("A"), "ACTIVE");
        assertThatThrownBy(() -> jdbc.update(insert, "M15.TEST_CATALOG", code, unique("B"), "INACTIVE"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void uniqueKey_isUniqueInEachTable() {
        String key = unique("K");
        jdbc.update(
                "INSERT INTO simple_record (kind, module, status, content, unique_key, created_at, created_by,"
                        + " version) VALUES ('M15.TEST_RECORD', 'M15', 'OPEN', '{}', ?, UTC_TIMESTAMP(6), 1, 0)",
                key);

        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO simple_record (kind, module, status, content, unique_key,"
                                + " created_at, created_by, version) VALUES ('M15.TEST_RECORD', 'M15', 'OPEN', '{}', ?,"
                                + " UTC_TIMESTAMP(6), 1, 0)",
                        key))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void technicalTables_appAccountCannotDelete() {
        jdbc.update(
                "INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at) VALUES"
                        + " ('M15.TEST', 'M15.TEST_ENTITY', ?, 'm', UTC_TIMESTAMP(6))",
                TestUsers.nextId());

        assertThatThrownBy(() -> jdbc.update("DELETE FROM reminder_marker WHERE kind = 'M15.TEST'"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM resident_module_state WHERE module = 'M15'"))
                .isInstanceOf(DataAccessException.class);
    }
}
