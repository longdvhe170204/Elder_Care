package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import java.sql.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/** T062: hàng thật đã commit, tài khoản admin cũng bị trigger chặn. */
class DBR_23_RecordTablesIT extends StructureTestSupport {
    @Test
    void DBR_23_appAndAdminCannotRewriteM03History() throws Exception {
        jdbc.update(
                "INSERT INTO bed_state_history(bed_id,to_state,event,occurred_at,basis_type,basis_id,floor_area_id) VALUES(?,'EMPTY','CREATE_BED',?,'TEST',1,?)",
                bed,
                Timestamp.from(time.now()),
                floor);
        jdbc.update(
                "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value,starts_at,actor_id,by_system,reason) VALUES('M03.ROOM',?,'ISOLATION','M03','POINT','ISOLATE',?,?,FALSE,'Kiểm tra trigger')",
                room,
                Timestamp.from(time.now()),
                user.accountId());
        try (var connection = adminConnection();
                var statement = connection.createStatement()) {
            // Lịch sử cách ly phòng là dòng status_history loại M03.ROOM / ISOLATION (R-15, gộp 2026-11-13)
            for (String table : List.of("bed_state_history", "status_history")) {
                String condition = table.equals("bed_state_history")
                        ? "bed_id=" + bed
                        : "entity_type='M03.ROOM' AND attribute='ISOLATION' AND entity_id=" + room;
                String update = "UPDATE " + table + " SET reason='Đổi nội dung' WHERE " + condition;
                String delete = "DELETE FROM " + table + " WHERE " + condition;
                assertThatThrownBy(() -> jdbc.update(update))
                        .isInstanceOf(org.springframework.dao.DataAccessException.class);
                assertThatThrownBy(() -> jdbc.update(delete))
                        .isInstanceOf(org.springframework.dao.DataAccessException.class);
                assertThatThrownBy(() -> statement.executeUpdate(update))
                        .isInstanceOf(SQLException.class)
                        .hasFieldOrPropertyWithValue("SQLState", "45000");
                assertThatThrownBy(() -> statement.executeUpdate(delete))
                        .isInstanceOf(SQLException.class)
                        .hasFieldOrPropertyWithValue("SQLState", "45000");
            }
        }
    }
}
