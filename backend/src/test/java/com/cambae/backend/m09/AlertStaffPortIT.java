package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m04.api.spi.CareShiftPort;
import com.cambae.backend.m05.api.spi.AlertStaffPort;
import com.cambae.backend.m07.api.spi.NursingShiftPort;
import com.cambae.backend.m09.api.ShiftApi;
import com.cambae.backend.m09.duty.internal.service.ShiftQueries;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
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
                "INSERT INTO shift_template(name,start_time,end_time,scope_type) VALUES ('[Kiểm SPI ca]','07:00','15:00','FLOOR')");
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

    @Test
    void Q_332_configuredOfficeHoursApplyOnlyToTheThreeNonScheduledRoles() {
        var hours = parameters.read(ParameterCode.CFG_M13_06).asTable(ShiftQueries.OfficeHours.class);
        var date = LocalDate.of(2026, 10, 19);
        while (!hours.days().contains(date.getDayOfWeek())) date = date.plusDays(1);
        Instant from = date.atTime(hours.from()).atZone(BusinessTime.ZONE).toInstant();
        Instant to = date.atTime(hours.to()).atZone(BusinessTime.ZONE).toInstant();
        for (var role : new Role[] {Role.NURSING_HOME_MANAGER, Role.OFFICE_STAFF, Role.ACCOUNTANT}) {
            long person = staff(role);
            assertThat(alerts.onShift(person, from.minusNanos(1000))).isFalse();
            assertThat(alerts.onShift(person, from)).isTrue();
            assertThat(alerts.onShift(person, to)).isFalse();
            assertThat(alerts.floorDuringShift(person, from)).isEmpty();
            assertThat(alerts.onShift(person, from)).isEqualTo(shifts.onShift(person, from));
        }
        assertThat(alerts.onShift(staff(Role.KITCHEN_STAFF), from)).isFalse();
    }

    @Test
    void FR_047i_draftAndCancelledShiftsNeverSupplyEmergencyRecipients() {
        long draft = staff(Role.NURSING_STAFF), cancelled = staff(Role.NURSING_STAFF);
        long draftFloor = TestUsers.nextId(), cancelledFloor = TestUsers.nextId();
        shift(draftFloor, draft, "DRAFT");
        shift(cancelledFloor, cancelled, "CANCELLED");
        assertThat(alerts.onShift(draft, time.now())).isFalse();
        assertThat(alerts.onShift(cancelled, time.now())).isFalse();
        assertThat(alerts.floorDuringShift(draft, time.now())).isEmpty();
        assertThat(alerts.floorsWithShift(time.now())).doesNotContain(draftFloor, cancelledFloor);
    }
}
