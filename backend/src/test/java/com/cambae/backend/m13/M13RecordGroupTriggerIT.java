package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.support.MySqlIntegrationTestBase;
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
 * 009 T036 (mở rộng {@code RecordGroupTriggerIT} của 000, tên logic {@code NhomGhiNhanCoTriggerIT}): M13 ghi bằng JDBC,
 * không có entity, nên liệt kê bảng nhóm 3 của 009 ở đây (data-model mục 1, FR-036): mỗi bảng có trigger chặn sửa, xóa;
 * tài khoản ứng dụng không sửa được.
 */
class M13RecordGroupTriggerIT extends MySqlIntegrationTestBase {

    static final List<String> RECORD_TABLES = List.of(
            "notification_request",
            "notification_request_content",
            "notification_request_recipient_group",
            "notification",
            "notification_event",
            "delivery_attempt",
            "delivery_result",
            "direct_contact_result",
            "call_reminder",
            "call_reminder_notification",
            "call_reminder_handoff",
            "call_result");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_036_everyM13RecordTable_hasBothTriggers() throws SQLException {
        Set<String> triggers = new HashSet<>();
        try (Connection c = adminConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT LOWER(TRIGGER_NAME) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE()")) {
            while (rs.next()) {
                triggers.add(rs.getString(1));
            }
        }

        assertThat(RECORD_TABLES)
                .allSatisfy(table ->
                        assertThat(triggers).contains("trg_" + table + "_no_update", "trg_" + table + "_no_delete"));
    }

    @Test
    void FR_036_appAccount_cannotUpdateOrDelete() {
        for (String table : RECORD_TABLES) {
            assertThatThrownBy(() -> jdbc.update("UPDATE " + table + " SET id = id WHERE id = -1"))
                    .as(table)
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update("DELETE FROM " + table + " WHERE id = -1"))
                    .as(table)
                    .isInstanceOf(DataAccessException.class);
        }
    }
}
