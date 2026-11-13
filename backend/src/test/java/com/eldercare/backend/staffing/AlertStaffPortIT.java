package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.care.api.spi.CareShiftPort;
import com.eldercare.backend.care.api.spi.TripStaffSchedulePort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.medication.api.spi.NursingShiftPort;
import com.eldercare.backend.staffing.api.ShiftApi;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T049/K1: dùng SPI ca thật, tài khoản thật; không nhập cấu hình fake của A/C/D. */
@Import(PostPublicationShiftIT.Time.class)
@Transactional
class AlertStaffPortIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    AlertStaffPort alerts;

    @Autowired
    ShiftApi shifts;

    @Autowired
    CareShiftPort care;

    @Autowired
    NursingShiftPort nursing;

    @Autowired
    TripStaffSchedulePort tripSchedules;

    @Autowired
    ParameterApi parameters;

    private final Instant start = Instant.parse("2026-10-19T00:00:00Z");

    @BeforeEach
    void freeze() {
        clock.setTo(start.plusSeconds(3600));
    }

    @AfterEach
    void reset() {
        clock.reset();
    }

    private long staff(Role role) {
        long max = jdbc.queryForObject("SELECT COALESCE(MAX(id),0) FROM staff_profile", Long.class);
        while (TestUsers.nextId() <= max) {}
        var user = new TestUsers(jdbc, time).staff(role);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Kiểm SPI ca]',?,?)",
                user.staffId(),
                role.name(),
                user.accountId());
        return user.staffId();
    }

    private long shift(long floor, long worker, String status) {
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(TRUE,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Kiểm SPI ca] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type) src");
        long schedule = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES ('2026-10','FLOOR',?,'PUBLISHED')",
                floor);
        long shift = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,status,requires_handover) VALUES (?,?,?,?,'FLOOR',?,?,TRUE)",
                schedule,
                template,
                ShiftStore.sql(start),
                ShiftStore.sql(start.plusSeconds(28800)),
                floor,
                status);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, worker);
        return shift;
    }

    @Test
    void FR_047i_K1_realPortsAgreeAtStartEndAndRecordedAbsence() {
        long worker = staff(Role.NURSING_STAFF), floor = TestUsers.nextId();
        long shift = shift(floor, worker, "PUBLISHED");
        assertThat(alerts.onShift(worker, start.minusNanos(1000))).isFalse();
        assertThat(alerts.onShift(worker, start)).isTrue();
        assertThat(alerts.floorDuringShift(worker, start)).contains(floor);
        assertThat(alerts.floorsWithShift(start)).contains(floor);
        assertThat(care.shiftAt(floor, start)).get().extracting(s -> s.id()).isEqualTo(shift);
        assertThat(nursing.nursesWithShift(floor, start)).containsExactly(worker);
        assertThat(alerts.onShift(worker, start.plusSeconds(28800))).isFalse();
        jdbc.update(
                "UPDATE shift_staff SET absent_at=? WHERE shift_id=? AND staff_id=?",
                ShiftStore.sql(time.now()),
                shift,
                worker);
        assertThat(alerts.onShift(worker, time.now().minusNanos(1000))).isTrue();
        assertThat(alerts.onShift(worker, time.now())).isFalse();
        assertThat(shifts.onShift(worker, time.now())).isFalse();
        assertThat(alerts.floorDuringShift(worker, time.now())).isEmpty();
        assertThat(nursing.inProgressShiftMember(worker, time.now())).isFalse();
        // Ca vẫn tồn tại dù một người vắng; danh sách tầng không phụ thuộc danh sách người còn lại.
        assertThat(alerts.floorsWithShift(time.now())).contains(floor);
    }
}
