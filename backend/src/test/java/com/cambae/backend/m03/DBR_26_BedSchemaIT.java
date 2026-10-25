package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** T007: thuộc tính trạng thái, xác nhận khử khuẩn và trigger chỉ thêm được kiểm trên MySQL thật. */
class DBR_26_BedSchemaIT extends MySqlIntegrationTestBase {
    private static final AtomicLong IDS = new AtomicLong(90370000);

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void requireSchemaAndPrepareRoom() {
        assertThat(jdbc.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE()", String.class))
                .contains("bed", "bed_state_history");
        jdbc.update("INSERT IGNORE INTO area(id,code,name) VALUES(903700,'M03-BED-A','Khu kiểm giường')");
        jdbc.update(
                "INSERT IGNORE INTO floor(id,area_id,code,name) VALUES(903700,903700,'M03-BED-T','Tầng kiểm giường')");
        jdbc.update("INSERT IGNORE INTO room_type(id,name,standard_beds) VALUES(903700,'Loại kiểm giường',2)");
        jdbc.update(
                "INSERT IGNORE INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(903700,903700,903700,'M03-BED-P','Phòng kiểm giường','UNRESTRICTED')");
    }

    @Test
    void DBR_26_bedStateAttributesAndCodesAreConstrained() {
        long id = bed();
        assertThatThrownBy(() -> jdbc.update("UPDATE bed SET state='HELD' WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () -> jdbc.update("UPDATE bed SET state='HELD',hold_reason='ABSENT_RESIDENT' WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE bed SET state='HELD',hold_reason='WAITING_RECORD' WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE bed SET state='AWAITING_READY' WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE bed SET state='SUSPENDED' WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        jdbc.update("UPDATE bed SET state='HELD',hold_reason='ABSENT_RESIDENT',hold_resident_id=1 WHERE id=?", id);
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, id))
                .isEqualTo("HELD");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO bed(room_id,code) VALUES(903700,?)", "G" + id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void BR_M03_10_nullOrMissingConfirmationCannotBypassDisinfectionCheck() {
        long id = bed();
        String sql =
                "INSERT INTO bed_state_history(bed_id,to_state,event,occurred_at,basis_type,basis_id,floor_area_id,disinfection_required,disinfected,ppe_used) VALUES(?,'EMPTY','CONFIRM_READY','2026-10-23 00:00:00','TEST',1,903700,?,?,?)";
        assertThatThrownBy(() -> jdbc.update(sql, id, null, null, null)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(sql, id, true, true, null)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(sql, id, true, false, true)).isInstanceOf(DataAccessException.class);
        assertThat(jdbc.update(sql, id, true, true, true)).isEqualTo(1);
    }

    @Test
    void DBR_23_historyRejectsUpdatesAndOrdinaryDeletesButAllowsRetention() throws Exception {
        long id = bed();
        jdbc.update(
                "INSERT INTO bed_state_history(bed_id,to_state,event,occurred_at,basis_type,basis_id,floor_area_id) VALUES(?,'EMPTY','CREATE_BED','2026-10-23 00:00:00','TEST',1,903700)",
                id);
        try (var admin = adminConnection();
                var command = admin.createStatement()) {
            assertThatThrownBy(
                            () -> command.executeUpdate("UPDATE bed_state_history SET reason='Đổi' WHERE bed_id=" + id))
                    .isInstanceOf(SQLException.class)
                    .satisfies(e -> assertThat(((SQLException) e).getSQLState()).isEqualTo("45000"));
            assertThatThrownBy(() -> command.executeUpdate("DELETE FROM bed_state_history WHERE bed_id=" + id))
                    .isInstanceOf(SQLException.class)
                    .satisfies(e -> assertThat(((SQLException) e).getSQLState()).isEqualTo("45000"));
        }
        try (var retention = retentionConnection();
                var command = retention.createStatement()) {
            assertThat(command.executeUpdate("DELETE FROM bed_state_history WHERE bed_id=" + id))
                    .isEqualTo(1);
        }
    }

    private long bed() {
        long id = IDS.incrementAndGet();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,903700,?)", id, "G" + id);
        return id;
    }
}
