package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.eldercare.backend.care.api.spi.CareShiftPort;
import com.eldercare.backend.care.api.spi.ShiftStaffPort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.medication.api.spi.NursingShiftPort;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.roombedallocation.api.spi.LocationReferences;
import com.eldercare.backend.staffing.api.ShiftApi;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;

/** T015: truy vấn thật theo giờ, không dựa vào việc job đã kịp chuyển trạng thái hay chưa. */
@Import(FakeM15Config.class)
class ShiftQueriesIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    ShiftApi shifts;

    @Autowired
    CareShiftPort care;

    @Autowired
    ShiftStaffPort careStaff;

    @Autowired
    NursingShiftPort nursing;

    @Autowired
    AlertStaffPort alerts;

    @Autowired
    LocationReferences locations;

    @Autowired
    com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore store;

    private long floor;

    @Test
    void FR_017_ongoingCareShiftsIncludeFloorsAndAreasWithoutBedsOrTasks() {
        long firstFloor = floor;
        long first = fixture(staff(Role.NURSING_STAFF), "PUBLISHED");
        floor = TestUsers.nextId();
        long second = fixture(staff(Role.NURSING_STAFF), "PUBLISHED");
        jdbc.update("UPDATE work_shift SET scope_type='AREA' WHERE id=?", second);
        assertThat(careStaff.ongoingShifts(time.now()))
                .filteredOn(s -> s.floorAreaId() == firstFloor || s.floorAreaId() == floor)
                .extracting(ShiftStaffPort.ShiftView::id)
                .containsExactlyInAnyOrder(first, second);
    }

    @Test
    void FR_017_ongoingCareShiftsUseHalfOpenTimeBounds() {
        long id = fixture(staff(Role.NURSING_STAFF), "PUBLISHED");
        assertThat(careStaff.ongoingShifts(Instant.parse("2031-01-13T00:00:00Z")))
                .extracting(ShiftStaffPort.ShiftView::id)
                .contains(id);
        assertThat(careStaff.ongoingShifts(Instant.parse("2031-01-13T08:00:00Z")))
                .extracting(ShiftStaffPort.ShiftView::id)
                .doesNotContain(id);
    }

    @Test
    void FR_017_ongoingCareShiftsExcludeUnpublishedAndFacilityShifts() {
        long draft = fixture(staff(Role.NURSING_STAFF), "DRAFT");
        floor = TestUsers.nextId();
        long cancelled = fixture(staff(Role.NURSING_STAFF), "CANCELLED");
        floor = TestUsers.nextId();
        long facility = fixture(staff(Role.NURSING_STAFF), "PUBLISHED");
        jdbc.update("UPDATE work_shift SET scope_type='FACILITY',scope_id=NULL WHERE id=?", facility);
        assertThat(careStaff.ongoingShifts(time.now()))
                .extracting(ShiftStaffPort.ShiftView::id)
                .doesNotContain(draft, cancelled, facility);
    }

    @BeforeEach
    void setup() {
        clock.setTo(Instant.parse("2031-01-13T03:00:00Z"));
        floor = TestUsers.nextId();
    }

    @AfterEach
    void reset() {
        clock.reset();
    }

    private long staff(Role role) {
        var user = new TestUsers(jdbc, time).staff(role);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?, '[Tra cứu ca]', 'Mô tả', ?)",
                user.staffId(),
                user.accountId());
        return user.staffId();
    }

    /** Cùng một kết nối cho mọi câu: LAST_INSERT_ID() tính theo kết nối, ngoài transaction mỗi câu có thể lấy kết nối khác của pool. */
    private long fixture(long worker, String status) {
        return transaction.execute(s -> insertFixture(worker, status));
    }

    private long insertFixture(long worker, String status) {
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(TRUE,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Tra cứu ca] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type) src");
        long schedule = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES ('2031-01','FLOOR',?,'PUBLISHED')",
                floor);
        long id = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status) VALUES (?,?,'2031-01-13 00:00','2031-01-13 08:00','FLOOR',?,TRUE,?)",
                schedule,
                template,
                floor,
                status);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", id, worker);
        return id;
    }

    private long doctorShift(long template, YearMonth month) {
        long schedule = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES (?,'FACILITY',NULL,'PUBLISHED')",
                month.toString());
        String day = month.atDay(13).toString();
        return AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status) VALUES (?,?,CONCAT(?,' 00:00'),CONCAT(?,' 08:00'),'FACILITY',NULL,FALSE,'PUBLISHED')",
                schedule,
                template,
                day,
                day);
    }

    @Test
    void FR_049_floorAndDoctorFixturesKeepTheirGeneratedKeysWhenConnectionsRotate() throws Exception {
        long worker = staff(Role.NURSING_STAFF);
        var originalJdbc = jdbc;
        try (var firstConnection = originalJdbc.getDataSource().getConnection();
                var secondConnection = originalJdbc.getDataSource().getConnection()) {
            var firstSource = new SingleConnectionDataSource(firstConnection, true);
            var secondSource = new SingleConnectionDataSource(secondConnection, true);
            var connections = List.of(firstSource.getConnection(), secondSource.getConnection());
            var nextConnection = new AtomicInteger();
            var rotatingSource = mock(DataSource.class);
            when(rotatingSource.getConnection())
                    .thenAnswer(invocation -> connections.get(nextConnection.getAndIncrement() % connections.size()));
            jdbc = new JdbcTemplate(rotatingSource);
            var secondJdbc = new JdbcTemplate(secondSource);
            secondJdbc.queryForObject("SELECT LAST_INSERT_ID(0)", Long.class);

            long floorShift = fixture(worker, "PUBLISHED");

            var created = store.find(floorShift).orElseThrow();
            assertThat(created.scopeType()).isEqualTo("FLOOR");
            assertThat(created.scopeId()).isEqualTo(floor);
            assertThat(shifts.staffOnShift(floor, time.now())).containsExactly(worker);

            var month = YearMonth.of(2031, 2);
            while (originalJdbc.queryForObject(
                            "SELECT COUNT(*) FROM shift_schedule WHERE month=? AND scope_type='FACILITY'",
                            Integer.class,
                            month.toString())
                    > 0) month = month.plusMonths(1);
            // Buộc cả hai helper đổi kết nối; không phụ thuộc pool trả lại kết nối nào.
            secondJdbc.queryForObject("SELECT LAST_INSERT_ID(0)", Long.class);
            nextConnection.set(0);
            long facilityShift = doctorShift(created.shiftTemplateId(), month);

            assertThat(store.find(facilityShift)).hasValueSatisfying(s -> {
                assertThat(s.scopeType()).isEqualTo("FACILITY");
                assertThat(s.scopeId()).isNull();
                assertThat(s.shiftTemplateId()).isEqualTo(created.shiftTemplateId());
            });
        } finally {
            jdbc = originalJdbc;
        }
    }

    @Test
    void FR_049_currentShiftAndStaffMembershipUseClockEvenWhenJobHasNotRun() {
        long worker = staff(Role.NURSING_STAFF);
        long id = fixture(worker, "PUBLISHED");
        var found = shifts.currentShift(floor, time.now()).orElseThrow();
        assertThat(found.id()).isEqualTo(id);
        assertThat(found.status()).isEqualTo("IN_PROGRESS");
        assertThat(shifts.onShift(worker, time.now())).isTrue();
        assertThat(care.shiftAt(floor, time.now())).isPresent();
        assertThat(nursing.nursesWithShift(floor, time.now())).containsExactly(worker);
        assertThat(alerts.floorDuringShift(worker, time.now())).contains(floor);
        assertThat(shifts.staff(worker).orElseThrow().fullName()).isEqualTo("[Tra cứu ca]");
    }

    @Test
    void FR_020_membershipIsHalfOpenAndStopsAtRecordedAbsence() {
        long worker = staff(Role.NURSING_STAFF);
        long id = fixture(worker, "PUBLISHED");
        assertThat(shifts.onShift(worker, Instant.parse("2031-01-12T23:59:59Z")))
                .isFalse();
        assertThat(shifts.onShift(worker, Instant.parse("2031-01-13T00:00:00Z")))
                .isTrue();
        assertThat(shifts.onShift(worker, Instant.parse("2031-01-13T08:00:00Z")))
                .isFalse();
        jdbc.update("UPDATE shift_staff SET absent_at='2031-01-13 03:00' WHERE shift_id=?", id);
        assertThat(shifts.onShift(worker, Instant.parse("2031-01-13T02:59:59Z")))
                .isTrue();
        assertThat(shifts.onShift(worker, time.now())).isFalse();
        assertThat(nursing.nursesWithShift(floor, time.now())).isEmpty();
    }

    @Test
    void FR_049_draftShiftDoesNotCreateScopeAndPastReferenceSurvivesClosure() {
        long worker = staff(Role.NURSING_STAFF);
        long id = fixture(worker, "DRAFT");
        assertThat(shifts.currentShift(floor, time.now())).isEmpty();
        assertThat(shifts.onShift(worker, time.now())).isFalse();
        var location = new LocationRef("FLOOR", floor);
        assertThat(locations.currentReferences(location)).hasSize(1);
        jdbc.update("UPDATE work_shift SET status='CLOSED' WHERE id=?", id);
        assertThat(locations.currentReferences(location)).isEmpty();
        assertThat(locations.everReferenced(location)).isTrue();
    }

    @Test
    void Q_332_nonScheduledRolesUseConfiguredOfficeHoursAndWeekdays() {
        long admin = staff(Role.OFFICE_STAFF);
        assertThat(shifts.onShift(admin, Instant.parse("2031-01-13T03:00:00Z"))).isTrue();
        assertThat(shifts.onShift(admin, Instant.parse("2031-01-13T12:00:00Z"))).isFalse();
        assertThat(shifts.onShift(admin, Instant.parse("2031-01-12T03:00:00Z"))).isFalse();
    }

    @Test
    void FR_013_recordedAbsenceKeepsTheWorkedIntervalForOverlapAndContinuousChainChecks() {
        long worker = staff(Role.NURSING_STAFF);
        long id = fixture(worker, "PUBLISHED");
        jdbc.update("UPDATE shift_staff SET absent_at='2031-01-13 03:00' WHERE shift_id=?", id);
        assertThat(store.forStaff(worker, null, false)).singleElement().satisfies(s -> {
            assertThat(s.startsAt()).isEqualTo(Instant.parse("2031-01-13T00:00:00Z"));
            assertThat(s.endsAt()).isEqualTo(Instant.parse("2031-01-13T03:00:00Z"));
        });
    }

    @Test
    void FR_020_staffOnShiftListsPresentMembersOfTheFloorShiftFor009() {
        long nurse = staff(Role.NURSING_STAFF);
        long caregiver = staff(Role.CAREGIVER);
        long id = fixture(nurse, "PUBLISHED");
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", id, caregiver);
        assertThat(shifts.staffOnShift(floor, time.now())).containsExactlyInAnyOrder(nurse, caregiver);
        jdbc.update(
                "UPDATE shift_staff SET absent_at='2031-01-13 02:00' WHERE shift_id=? AND staff_id=?", id, caregiver);
        assertThat(shifts.staffOnShift(floor, time.now())).containsExactly(nurse);
        assertThat(shifts.staffOnShift(floor, Instant.parse("2031-01-13T08:00:00Z")))
                .isEmpty();
        assertThat(shifts.staffOnShift(TestUsers.nextId(), time.now())).isEmpty();
    }

    @Test
    void FR_023_onDutyDoctorsArePresentDoctorsMarkedOnDutyFor009() {
        long nurse = staff(Role.NURSING_STAFF);
        long doctor = staff(Role.DOCTOR);
        long other = staff(Role.DOCTOR);
        long floorShift = fixture(nurse, "PUBLISHED");
        // Bác sĩ trực ở ca cấp viện; dấu Bác sĩ trực trên ca tầng không tính
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id,on_duty_doctor) VALUES (?,?,TRUE)", floorShift, other);
        long template = jdbc.queryForObject("SELECT template_id FROM work_shift WHERE id=?", Long.class, floorShift);
        long schedule = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES ('2031-01','FACILITY',NULL,'PUBLISHED')");
        long id = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status) VALUES (?,?,'2031-01-13 00:00','2031-01-13 08:00','FACILITY',NULL,FALSE,'PUBLISHED')",
                schedule,
                template);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id,on_duty_doctor) VALUES (?,?,TRUE)", id, doctor);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", id, other);
        assertThat(shifts.onDutyDoctors(time.now())).contains(doctor).doesNotContain(other, nurse);
        jdbc.update("UPDATE shift_staff SET absent_at='2031-01-13 02:00' WHERE shift_id=? AND staff_id=?", id, doctor);
        assertThat(shifts.onDutyDoctors(time.now())).doesNotContain(doctor);
    }
}
