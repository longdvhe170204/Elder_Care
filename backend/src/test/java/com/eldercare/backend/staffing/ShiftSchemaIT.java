package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
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
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(src.requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Mẫu kiểm thử] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'19:00' AS end_time,'FACILITY' AS scope_type,FALSE AS requires_handover) src");
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
                "INSERT INTO record_event(entity_type,entity_id,module,kind,by_system,occurred_at,confirmed_at,content) SELECT 'M09.SCHEDULE',src.schedule_id,'M09','M09.SHIFT_PUBLICATION',FALSE,src.published_at,src.published_at,JSON_OBJECT('content',CAST(src.content AS JSON)) FROM (SELECT ? AS schedule_id,'2026-10-24' AS published_at,'{}' AS content) src",
                schedule);
        long id = lastId();
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE record_event e SET content=JSON_SET(content,'$.content',CAST('[]' AS JSON)) WHERE e.kind='M09.SHIFT_PUBLICATION' AND (COALESCE(CAST(NULLIF(JSON_UNQUOTE(e.content->'$.legacy.id'),'null') AS UNSIGNED),e.id)=?)",
                        id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "DELETE FROM record_event e WHERE e.kind='M09.SHIFT_PUBLICATION' AND (COALESCE(CAST(NULLIF(JSON_UNQUOTE(e.content->'$.legacy.id'),'null') AS UNSIGNED),e.id)=?)",
                        id))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(e.content->'$.legacy.id'),'null') AS UNSIGNED),e.id) AS id,e.entity_id AS schedule_id,e.occurred_at AS published_at,e.content->'$.content' AS content FROM record_event e WHERE e.kind='M09.SHIFT_PUBLICATION') shift_publication WHERE id=?",
                        Integer.class,
                        id))
                .isOne();
    }
}
