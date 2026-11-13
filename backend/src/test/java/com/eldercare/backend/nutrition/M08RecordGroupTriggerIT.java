package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * 011 T041, T046 (mở rộng {@code RecordGroupTriggerIT} của 000 cho bảng nhóm 3 của 011; data-model mục 0.1, constitution
 * XII): lần chốt suất (loại {@code M08.PORTION_CLOSING}, gộp bảng {@code portion_closing} 2026-11-13) và các bản ghi nhóm 3 khác của 011 (phát
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

    private long portionClosing() {
        return confirmedEvent("M08.PORTION_CLOSING");
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
    void BR_35_portionClosing_appAccountCannotUpdateOrDelete() throws SQLException {
        long id = portionClosing();

        assertThatThrownBy(
                        () -> jdbc.update("UPDATE record_event SET content = '{\"totalPortions\":9}' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM record_event WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
    }
}
