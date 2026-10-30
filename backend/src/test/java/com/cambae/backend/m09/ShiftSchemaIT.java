package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T011/T012: ràng buộc MySQL thật của lịch, ca và lịch sử; mọi fixture hoàn tác sau test. */
@Transactional
class ShiftSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    private long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long template() {
        jdbc.update(
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,requires_handover) VALUES ('[Mẫu kiểm thử]','07:00','19:00','FACILITY',FALSE)");
        return lastId();
    }

    private long schedule(String month) {
        jdbc.update("INSERT INTO shift_schedule(month,scope_type) VALUES (?,'FACILITY')", month);
        return lastId();
    }

    private long shift(long schedule, long template) {
        jdbc.update(
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,requires_handover) VALUES (?,?,'2026-11-01 00:00','2026-11-01 12:00','FACILITY',FALSE)",
                schedule,
                template);
        return lastId();
    }

    @Test
    void FR_012_facilityScheduleIsUniqueEvenWhenScopeIdIsNull() {
        schedule("2026-11");
        assertThatThrownBy(() -> schedule("2026-11")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void BR_M09_12_shiftAllows24HoursAndRejectsZeroOrMoreThan24Hours() {
        long schedule = schedule("2026-12");
        long template = template();
        long shift = shift(schedule, template);
        jdbc.update("UPDATE work_shift SET ends_at='2026-11-02 00:00' WHERE id=?", shift);
        assertThatThrownBy(() -> jdbc.update("UPDATE work_shift SET ends_at=starts_at WHERE id=?", shift))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE work_shift SET ends_at='2026-11-02 00:00:01' WHERE id=?", shift))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_017_publishedHoursAndClosedShiftContentsAreImmutable() {
        long shift = shift(schedule("2027-01"), template());
        jdbc.update("UPDATE work_shift SET status='PUBLISHED' WHERE id=?", shift);
        assertThatThrownBy(() -> jdbc.update("UPDATE work_shift SET ends_at='2026-11-01 13:00' WHERE id=?", shift))
                .isInstanceOf(DataAccessException.class);
        jdbc.update("UPDATE work_shift SET status='CLOSED' WHERE id=?", shift);
        assertThatThrownBy(() -> jdbc.update("UPDATE work_shift SET missing_lead=TRUE WHERE id=?", shift))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_055_publicationSnapshotCannotBeEditedOrDeletedByApplication() {
        long schedule = schedule("2027-02");
        jdbc.update(
                "INSERT INTO shift_publication(schedule_id,published_at,content) VALUES (?, '2026-10-24', '{}')",
                schedule);
        long id = lastId();
        assertThatThrownBy(() -> jdbc.update("UPDATE shift_publication SET content='[]' WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM shift_publication WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM shift_publication WHERE id=?", Integer.class, id))
                .isOne();
    }
}
