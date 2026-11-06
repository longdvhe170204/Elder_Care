package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T018/T019: ràng buộc MySQL thật, giữ bản ghi phân công sau công bố và sau trạng thái cuối. */
@Transactional
class AssignmentSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    private long id() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long staff() {
        jdbc.update("INSERT INTO staff_profile(full_name,position) VALUES ('[Nhân viên phân công]','Điều dưỡng')");
        return id();
    }

    private long shift() {
        jdbc.update(
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(TRUE,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Mẫu phân công] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type) src");
        long template = id();
        jdbc.update("INSERT INTO shift_schedule(month,scope_type,scope_id) VALUES ('2032-01','FLOOR',?)", template);
        long schedule = id();
        jdbc.update(
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover) VALUES (?,?,'2032-01-01','2032-01-01 08:00','FLOOR',?,TRUE)",
                schedule,
                template,
                template);
        return id();
    }

    private long assignment() {
        jdbc.update(
                "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,role,assignment_kind,starts_at,ends_at) VALUES (?,?,'ROOM',21,'NURSING_STAFF','PRIMARY','2032-01-01','2032-01-01 08:00')",
                shift(),
                staff());
        return id();
    }

    @Test
    void FR_025_assignmentStartsAsDraftAndCannotHaveAnEmptyInterval() {
        long id = assignment();
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, id))
                .isEqualTo("DRAFT");
        assertThatThrownBy(() -> jdbc.update("UPDATE staff_assignment SET ends_at=starts_at WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_027_terminalAssignmentIsImmutableAndCannotBeDeletedByApplication() {
        long id = assignment();
        jdbc.update("UPDATE staff_assignment SET status='CANCELLED' WHERE id=?", id);
        assertThatThrownBy(() -> jdbc.update("UPDATE staff_assignment SET target_id=22 WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM staff_assignment WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_025_deleteIsOnlyAllowedWhileParentShiftIsDraft() {
        long id = assignment();
        long shift = jdbc.queryForObject("SELECT shift_id FROM staff_assignment WHERE id=?", Long.class, id);
        jdbc.update("UPDATE work_shift SET status='PUBLISHED' WHERE id=?", shift);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM staff_assignment WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_024_floorManagerIntervalMustBePositiveAndScopeLockIsUnique() {
        jdbc.update(
                "INSERT INTO technical_lock(lock_key,version,updated_at) SELECT CONCAT('M09.TANG:',src.floor_area_id),0,'1970-01-01 00:00:00' FROM (SELECT 912345 AS floor_area_id) src");
        assertThatThrownBy(
                        () -> jdbc.update(
                                "INSERT INTO technical_lock(lock_key,version,updated_at) SELECT CONCAT('M09.TANG:',src.floor_area_id),0,'1970-01-01 00:00:00' FROM (SELECT 912345 AS floor_area_id) src"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO staff_assignment(kind,staff_id,floor_area_id,starts_at,ends_at,assigned_by,reason,status) SELECT 'FLOOR_MANAGER',src.staff_id,src.floor_area_id,src.starts_at,src.ends_at,1,'[Khởi tạo fixture]','ACTIVE' FROM (SELECT ? AS staff_id,912345 AS floor_area_id,'2032-01-02' AS starts_at,'2032-01-01' AS ends_at) src",
                        staff()))
                .isInstanceOf(DataAccessException.class);
    }
}
