package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
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
 * 009 T036, T043 (mở rộng {@code RecordGroupTriggerIT} của 000; data-model mục 0.1, FR-036): sau gộp bảng R-15, yêu cầu
 * thông báo là nhóm 3 (trigger chặn sửa, xóa); thông báo và lượt nhắc gọi khóa theo dòng: chỉ cột trạng thái dẫn xuất
 * sửa được (quyền theo cột của {@code eldercare_app}, trigger {@code row_lock} chặn mọi cột khác), không xóa. Sự kiện,
 * lần gửi, kết quả nằm ở {@code record_event} (trigger của 000).
 */
class M13RecordGroupTriggerIT extends MySqlIntegrationTestBase {

    static final List<String> RECORD_TABLES = List.of("notification_request");

    static final List<String> ROW_LOCKED_TABLES = List.of("notification", "call_reminder");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_036_m13Tables_haveTriggers() throws SQLException {
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
        assertThat(ROW_LOCKED_TABLES)
                .allSatisfy(table ->
                        assertThat(triggers).contains("trg_" + table + "_row_lock", "trg_" + table + "_no_delete"));
    }

    @Test
    void FR_036_appAccount_cannotUpdateLockedColumnsOrDelete() {
        for (String table : RECORD_TABLES) {
            assertThatThrownBy(() -> jdbc.update("UPDATE " + table + " SET id = id WHERE id = -1"))
                    .as(table)
                    .isInstanceOf(DataAccessException.class);
        }
        assertThatThrownBy(() -> jdbc.update("UPDATE notification SET basis = basis WHERE id = -1"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE call_reminder SET callee_id = callee_id WHERE id = -1"))
                .isInstanceOf(DataAccessException.class);
        for (String table : List.of("notification_request", "notification", "call_reminder")) {
            assertThatThrownBy(() -> jdbc.update("DELETE FROM " + table + " WHERE id = -1"))
                    .as(table)
                    .isInstanceOf(DataAccessException.class);
        }
        // Cột trạng thái dẫn xuất sửa được
        jdbc.update("UPDATE notification SET hidden = hidden WHERE id = -1");
        jdbc.update("UPDATE call_reminder SET due_at = due_at WHERE id = -1");
    }

    @Test
    void XII_rowLock_blocksOtherColumns_evenForAdmin() throws SQLException {
        long request =
                jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) + 1000000 FROM notification_request", Long.class);
        try (Connection c = adminConnection();
                Statement s = c.createStatement()) {
            s.executeUpdate("INSERT INTO notification_request (id, level, received_at, intake_result, contents,"
                    + " recipient_groups) VALUES (" + request + ", 'LOW', UTC_TIMESTAMP(6), 'VALID', JSON_ARRAY(),"
                    + " JSON_ARRAY())");
            s.executeUpdate("INSERT INTO notification (request_id, recipient_type, recipient_id, basis, level,"
                    + " created_at) VALUES (" + request + ", 'STAFF', 1, '[]', 'LOW', UTC_TIMESTAMP(6))");
            s.executeUpdate("UPDATE notification SET status = 'SENT', sent_at = UTC_TIMESTAMP(6) WHERE request_id = "
                    + request);
            assertThatThrownBy(() ->
                            s.executeUpdate("UPDATE notification SET recipient_id = 2 WHERE request_id = " + request))
                    .isInstanceOf(SQLException.class)
                    .hasFieldOrPropertyWithValue("SQLState", "45000");
        }
    }

    @Test
    void FR_032_onePendingCallReminderPerCallee() {
        String callee = "STAFF:" + System.nanoTime() + ":resident:1";
        String insert = "INSERT INTO call_reminder (callee_type, callee_id, resident_id, creation_reason, created_at,"
                + " status, due_at, current_staff_id, callee_key) VALUES ('STAFF', 1, 1, 'UNACKNOWLEDGED',"
                + " UTC_TIMESTAMP(6), ?, UTC_TIMESTAMP(6), 1, ?)";
        jdbc.update(insert, "AWAITING_RESULT", callee);
        jdbc.update(insert, "RESULT_RECORDED", callee);

        assertThatThrownBy(() -> jdbc.update(insert, "AWAITING_RESULT", callee))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("pending");
    }
}
