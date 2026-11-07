package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.List;
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
}
