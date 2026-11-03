package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.support.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T027/T028: ràng buộc loại M04 trong bảng chung, không tạo bảng riêng. */
@Transactional
class FR_012_AttendanceSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    private void insert(String kind, long resident, String state) {
        jdbc.update(
                "INSERT INTO simple_record(kind,module,resident_id,status,content,effective_from,created_by,created_at) VALUES(?,'M04',?,?,'{\"expectedArrival\":\"08:00:00\"}','2026-11-01',1,'2026-11-01')",
                kind,
                resident,
                state);
    }

    @Test
    void FR_012_databaseEnforcesOneAttendancePerResidentDateWithoutRelyingOnClientKey() {
        long resident = TestUsers.nextId();
        insert("M04.DAY_CARE_ATTENDANCE", resident, "NOT_ARRIVED");
        assertThatThrownBy(() -> insert("M04.DAY_CARE_ATTENDANCE", resident, "PRESENT"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_013_databaseRejectsUnknownAttendanceStates() {
        assertThatThrownBy(() -> insert("M04.DAY_CARE_ATTENDANCE", TestUsers.nextId(), "UNKNOWN"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_012_moduleConstraintsLeaveOtherSharedKindsAlone() {
        long resident = TestUsers.nextId();
        insert("M04.SCHEMA_TEST", resident, "OTHER");
        insert("M04.SCHEMA_TEST", resident, "OTHER");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE resident_id=?", Integer.class, resident))
                .isEqualTo(2);
    }

    @Test
    void FR_013_adHocSessionMayHaveNoExpectedArrival() {
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO simple_record(kind,module,resident_id,status,content,effective_from,created_by,created_at) VALUES('M04.DAY_CARE_ATTENDANCE','M04',?,'PRESENT','{\"expectedArrival\":null}','2026-11-01',1,'2026-11-01')",
                resident);
        assertThat(jdbc.queryForObject(
                        "SELECT m04_expected_arrival FROM simple_record WHERE kind='M04.DAY_CARE_ATTENDANCE' AND resident_id=?",
                        String.class,
                        resident))
                .isNull();
    }

    @Test
    void FR_012_bothDueAndDailyIndexesExistOnTheSharedTable() {
        var indexes = jdbc.queryForList(
                "SELECT DISTINCT index_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='simple_record' AND index_name LIKE 'ix_m04_attendance_%'",
                String.class);
        assertThat(indexes).contains("ix_m04_attendance_due", "ix_m04_attendance_day");
    }
}
