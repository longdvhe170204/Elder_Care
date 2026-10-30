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
                "INSERT INTO room_isolation_history(room_id,action,occurred_at,actor_id,reason) VALUES(?,'ISOLATE',?,?,'Kiểm tra trigger')",
                room,
                Timestamp.from(time.now()),
                user.accountId());
        try (var connection = adminConnection();
                var statement = connection.createStatement()) {
            for (String table : List.of("bed_state_history", "room_isolation_history")) {
                String condition = table.equals("bed_state_history") ? "bed_id=" + bed : "room_id=" + room;
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
