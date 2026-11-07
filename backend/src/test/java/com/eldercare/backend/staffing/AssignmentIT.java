package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.care.api.spi.CareShiftPort;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.*;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.medication.api.spi.NursingShiftPort;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.staffing.internal.assignment.domain.AssignmentInput;
import com.eldercare.backend.staffing.internal.assignment.service.*;
import com.eldercare.backend.staffing.internal.shift.service.ShiftScheduleService;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.fake.care.*;
import com.eldercare.backend.support.fake.resident.*;
import com.eldercare.backend.support.fake.roombedallocation.*;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.*;

/** T018/T020/T021: lệnh thật, MySQL thật, chỉ nguồn B/C dùng bản giả chung. */
@Import({FakeM01Config.class, FakeLocations.Config.class, FakeM04Config.class, SwitchableUserConfig.class})
@RecordApplicationEvents
class AssignmentIT extends MySqlIntegrationTestBase {
    private static final java.util.concurrent.atomic.AtomicInteger sequence =
            new java.util.concurrent.atomic.AtomicInteger();

    @Autowired
    AssignmentService service;

    @Autowired
    FloorManagerService managers;

    @Autowired
    AssignmentApi assignments;

    @Autowired
    DutyApi duties;

    @Autowired
    ShiftScheduleService schedules;

    @Autowired
    AssignmentLifecycle lifecycle;

    @Autowired
    ResidentFinalAssignmentEffects residentEffects;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    SwitchableUser current;

    @Autowired
    FakeLocations locations;

    @Autowired
    FakeResidents residents;

    @Autowired
    FakeCareRequirements tasks;

    @Autowired
    @Qualifier("careShiftLookup")
    CareShiftPort care;

    @Autowired
    NursingShiftPort nursing;

    @Autowired
    AlertStaffPort alerts;

    @Autowired
    ApplicationEvents applicationEvents;

    @Autowired
    com.eldercare.backend.staffing.internal.service.M09Jobs jobs;

    @Autowired
    org.springframework.context.ApplicationEventPublisher publisher;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    private long floor, room, resident, worker, shift, schedule;
    private Instant from, to;

    @BeforeEach
    void setup() {
        // Đặt đồng hồ trước khi tạo người dùng: quyền của Quản lý viện phải có hiệu lực từ trước mốc đồng hồ của test
        clock.setTo(Instant.parse("2026-11-01T01:00:00Z"));
        current.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
        floor = TestUsers.nextId();
        room = TestUsers.nextId();
        resident = TestUsers.nextId();
        locations.floor(floor, TestUsers.nextId()).room(room, floor);
        from = Instant.parse("2026-11-01T02:00:00Z").plusSeconds(60L * sequence.incrementAndGet());
        to = from.plusSeconds(8 * 3600);
        locations.stay(resident, room, from.minusSeconds(7200), null);
        residents.resident(resident, "[Họ tên NCT kiểm thử]", null, ResidentStatus.IN_RESIDENCE);
        worker = staff(Role.CAREGIVER);
        shift = shift(worker, "DRAFT", from, to);
        tasks.failOnEnd(false);
    }

    @AfterEach
    void reset() {
        clock.reset();
        tasks.failOnEnd(false);
    }

    private long insert(String sql, Object... args) {
        return AssignmentFixtures.insert(jdbc, sql, args);
    }

    private long staff(Role role) {
        var account = new TestUsers(jdbc, time).staff(role);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Nhân viên kiểm thử phân công]','Chức danh',?)",
                account.staffId(),
                account.accountId());
        return account.staffId();
    }

    private long shift(long person, String status, Instant starts, Instant ends) {
        long template = insert(
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(src.requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Ca phân công] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type,FALSE AS requires_handover) src");
        schedule = insert(
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status,created_by) VALUES (?,'FLOOR',?,?,?)",
                YearMonth.from(starts.atZone(BusinessTime.ZONE)).toString(),
                floor,
                "DRAFT".equals(status) ? "DRAFT" : "PUBLISHED",
                current.accountId());
        long id = insert(
                "INSERT INTO work_shift(schedule_id,template_id,scope_type,scope_id,starts_at,ends_at,requires_handover,status) VALUES (?,?,'FLOOR',?,?,?,FALSE,?)",
                schedule,
                template,
                floor,
                com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore.sql(starts),
                com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore.sql(ends),
                status);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", id, person);
        return id;
    }

    private AssignmentInput input(long person, String type, long target, Long task, Role role, String kind) {
        return new AssignmentInput(person, type, target, task, role, kind, false, null, null);
    }

    private AssignmentInput usual() {
        return input(worker, "FLOOR", floor, null, Role.CAREGIVER, "PRIMARY");
    }

    private Target target() {
        return new Target("M09.SHIFT", Long.toString(shift), null, floor);
    }

    private long create(AssignmentInput input) {
        return service.create(shift, input, target()).id();
    }

    private void published() {
        jdbc.update("UPDATE shift_schedule SET status='PUBLISHED' WHERE id=?", schedule);
        jdbc.update("UPDATE work_shift SET status='PUBLISHED' WHERE id=?", shift);
    }

    @Autowired
    org.springframework.context.ApplicationContext context;

    @Test
    void FR_025_draftDoesNotGrantScopeAndPublishedAssignmentDoes() {
        long id = create(usual());
        assertThat(assignments.history(worker, from, to)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, id))
                .isEqualTo("DRAFT");
        service.delete(id, target());
        published();
        create(usual());
        assertThat(assignments.history(worker, from, to)).anyMatch(a -> Objects.equals(a.floorAreaId(), floor));
        assertThat(assignments.activeAt(worker, from)).hasSize(1);
        assertThat(assignments.activeAt(worker, to)).isEmpty();
    }

    private void credential(long person, LocalDate expires) {
        jdbc.update(
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date) SELECT ?,id,?,? FROM (SELECT c.id AS id,c.name AS name,c.status AS status,c.version AS version,CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(c.attributes->'$.createdAt'),'null'),'T',' '),'Z','') AS DATETIME(6)) AS created_at,COALESCE(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacyCode'),'null'),c.code) AS code,NULLIF(JSON_UNQUOTE(c.attributes->'$.credentialGroup'),'null') AS credential_group,(c.attributes->'$.hasExpiry'=CAST('true' AS JSON)) AS has_expiry,c.attributes->'$.requiredForRoles' AS required_for_roles,(c.attributes->'$.hasPracticeScopes'=CAST('true' AS JSON)) AS has_practice_scopes FROM catalog_item c WHERE c.catalog_type='M09.CREDENTIAL_TYPE') credential_type WHERE code='FIRST_AID_TRAINING'",
                person,
                expires.minusDays(30),
                expires);
    }

    private long anotherShift(long person, Instant start, Instant end) {
        long template = jdbc.queryForObject("SELECT template_id FROM work_shift WHERE id=?", Long.class, shift);
        long result = insert(
                "INSERT INTO work_shift(schedule_id,template_id,scope_type,scope_id,starts_at,ends_at,requires_handover) VALUES (?,?,'FLOOR',?,?,?,FALSE)",
                schedule,
                template,
                floor,
                com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore.sql(start),
                com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore.sql(end));
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", result, person);
        return result;
    }

    @Test
    void FR_028_concurrentPrimaryCommandsOnlyOneSucceeds() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var barrier = new CyclicBarrier(2);
        try {
            var a = pool.submit(() -> race(barrier));
            var b = pool.submit(() -> race(barrier));
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean race(CyclicBarrier barrier) throws Exception {
        barrier.await();
        try {
            create(usual());
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    @Test
    void FR_027_earlyEndIsAtomicWithTaskEffectAndStopsScopeImmediately() {
        published();
        long id = create(usual());
        clock.setTo(from.plusSeconds(60));
        lifecycle.run(time.now(), time.now());
        tasks.failOnEnd(true);
        assertThatThrownBy(() -> service.end(id, new Reason("Đổi nhân viên"), target()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, id))
                .isEqualTo("ACTIVE");
        tasks.failOnEnd(false);
        service.end(id, new Reason("Đổi nhân viên"), target());
        assertThat(tasks.endedAssignments()).contains(id);
        var cut = jdbc.queryForObject(
                        "SELECT actual_end_at FROM staff_assignment WHERE id=?", java.time.LocalDateTime.class, id)
                .toInstant(ZoneOffset.UTC);
        assertThat(assignments.history(worker, from, to)).anyMatch(a -> cut.equals(a.scopeEndsAt()));
        assertThat(assignments.activeAt(worker, time.now())).isEmpty();
    }

    private boolean floorRace(long staff, CyclicBarrier barrier) throws Exception {
        barrier.await();
        try {
            managers.assign(staff, floor, time.today(), time.today().plusDays(2), null, target());
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    private void readyToPublish() {
        long manager = staff(Role.FLOOR_MANAGER);
        managers.assign(manager, floor, time.today(), null, null, target());
        long lead = staff(Role.NURSING_STAFF);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, lead);
        jdbc.update("UPDATE work_shift SET lead_staff_id=? WHERE id=?", lead, shift);
    }

    @Autowired
    @Qualifier("shiftLocationReferences")
    com.eldercare.backend.roombedallocation.api.spi.LocationReferences locationReferences;
}
