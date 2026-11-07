package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
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
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * 011 T041, T046 (mở rộng {@code RecordGroupTriggerIT} của 000 cho bảng nhóm 3 của 011; data-model mục 0.1, constitution
 * XII): bảng riêng {@code portion_closing} có trigger chặn sửa, xóa; sau gộp R-15 các bản ghi nhóm 3 khác của 011 (phát
 * sinh, yêu cầu suất bổ sung, phần bổ sung, sai lệch phiếu, xác nhận phục vụ, đổi món, kiểm tra thực đơn) là dòng
 * {@code record_event} đã xác nhận, trigger của 000 chặn sửa, xóa, kể cả tài khoản quản trị.
 */
class M08RecordGroupTriggerIT extends MySqlIntegrationTestBase {

    /** Loại {@code record_event} nhóm 3 của 011. */
    static final List<String> RECORD_EVENT_KINDS = List.of(
            "M08.ADJUSTMENT",
            "M08.EXTRA_PORTION_REQUEST",
            "M08.SUPPLEMENTARY_PORTION",
            "M08.SLIP_DISCREPANCY",
            "M08.SERVING_CONFIRMATION",
            "M08.DISH_SWAP",
            "M08.MENU_CHECK");

    @Autowired
    JdbcTemplate jdbc;

    private long portionClosing() throws SQLException {
        long dailyMeal = TestUsers.nextId();
        try (Connection c = adminConnection();
                Statement s = c.createStatement()) {
            s.executeUpdate(
                    "INSERT INTO portion_closing (daily_meal_id, meal_id, meal_date, meal_time, late_threshold_minutes,"
                            + " closed_by, closed_at, closed_after_meal_time, menu_id, total_portions, counted_residents,"
                            + " portions_by_diet, flags, missing_data) VALUES ("
                            + dailyMeal
                            + ", "
                            + TestUsers.nextId()
                            + ", '2001-03-05', '2001-03-05 04:00:00', 30, 1, UTC_TIMESTAMP(6), FALSE, NULL, 0,"
                            + " JSON_ARRAY(), JSON_ARRAY(), JSON_ARRAY(), JSON_ARRAY())",
                    Statement.RETURN_GENERATED_KEYS);
            try (ResultSet keys = s.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private long confirmedEvent(String kind) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(
                            "INSERT INTO record_event (entity_type, entity_id, module, kind, content, actor_id, by_system,"
                                    + " occurred_at, confirmed_at) VALUES ('M08.DAILY_MEAL', ?, 'M08', ?, '{}', 1, FALSE,"
                                    + " UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))",
                            new String[] {"id"});
                    ps.setLong(1, TestUsers.nextId());
                    ps.setString(2, kind);
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    @Test
    void XII_portionClosing_hasBothAppendOnlyTriggers() throws SQLException {
        Set<String> triggers = new HashSet<>();
        try (Connection c = adminConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT LOWER(TRIGGER_NAME) FROM information_schema.TRIGGERS"
                        + " WHERE TRIGGER_SCHEMA = DATABASE() AND EVENT_OBJECT_TABLE = 'portion_closing'")) {
            while (rs.next()) {
                triggers.add(rs.getString(1));
            }
        }

        assertThat(triggers).contains("trg_portion_closing_no_update", "trg_portion_closing_no_delete");
    }

    @Test
    void BR_M08_01_portionClosing_appAccountCannotUpdateOrDelete() throws SQLException {
        long id = portionClosing();

        assertThatThrownBy(() -> jdbc.update("UPDATE portion_closing SET total_portions = 9 WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM portion_closing WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void BR_M08_01_portionClosing_evenAdminIsBlockedByTriggers() throws SQLException {
        long id = portionClosing();
        try (Connection c = adminConnection();
                Statement s = c.createStatement()) {
            assertThatThrownBy(() -> s.executeUpdate("UPDATE portion_closing SET total_portions = 9 WHERE id = " + id))
                    .isInstanceOf(SQLException.class)
                    .hasFieldOrPropertyWithValue("SQLState", "45000");
            assertThatThrownBy(() -> s.executeUpdate("DELETE FROM portion_closing WHERE id = " + id))
                    .isInstanceOf(SQLException.class)
                    .hasFieldOrPropertyWithValue("SQLState", "45000");
        }
    }

    @Test
    void XII_confirmedRecordEvents_ofM08_cannotBeChangedOrDeleted() throws SQLException {
        for (String kind : RECORD_EVENT_KINDS) {
            long id = confirmedEvent(kind);

            assertThatThrownBy(() -> jdbc.update("UPDATE record_event SET actor_id = 2 WHERE id = ?", id))
                    .as(kind)
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("append-only");
            assertThatThrownBy(() -> jdbc.update("DELETE FROM record_event WHERE id = ?", id))
                    .as(kind)
                    .isInstanceOf(DataAccessException.class);
            try (Connection c = adminConnection();
                    Statement s = c.createStatement()) {
                assertThatThrownBy(() -> s.executeUpdate("DELETE FROM record_event WHERE id = " + id))
                        .as(kind)
                        .isInstanceOf(SQLException.class)
                        .hasFieldOrPropertyWithValue("SQLState", "45000");
            }
        }
    }
}
