package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.*;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.*;
import com.cambae.backend.common.time.*;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m03.api.dto.LocationRef;
import com.cambae.backend.m04.api.dto.TaskRequirements;
import com.cambae.backend.m04.api.spi.CareShiftPort;
import com.cambae.backend.m05.api.spi.AlertStaffPort;
import com.cambae.backend.m07.api.spi.NursingShiftPort;
import com.cambae.backend.m09.assignment.internal.domain.AssignmentInput;
import com.cambae.backend.m09.assignment.internal.service.*;
import com.cambae.backend.m09.shift.internal.service.ShiftScheduleService;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.*;
import com.cambae.backend.support.*;
import com.cambae.backend.support.fake.m01.*;
import com.cambae.backend.support.fake.m03.*;
import com.cambae.backend.support.fake.m04.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.*;

/** T018/T020/T021: lệnh thật, MySQL thật, chỉ nguồn B/C dùng bản giả chung. */
@Import({FakeM01Config.class, FakeM03Config.class, FakeM04Config.class, AssignmentIT.Users.class})
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
    com.cambae.backend.m09.internal.service.M09Jobs jobs;

    @Autowired
    org.springframework.context.ApplicationEventPublisher publisher;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    private long floor, room, resident, worker, shift, schedule;
    private Instant from, to;

    @BeforeEach
    void setup() {
        current.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
        clock.setTo(Instant.parse("2026-11-01T01:00:00Z"));
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
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,requires_handover) VALUES ('[Ca phân công]','07:00','15:00','FLOOR',FALSE)");
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
                com.cambae.backend.m09.shift.internal.persistence.ShiftStore.sql(starts),
                com.cambae.backend.m09.shift.internal.persistence.ShiftStore.sql(ends),
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

    @Test
    void FR_025_missingOrAbsentMemberAndInactiveTargetAreRejected() {
        assertThatThrownBy(() -> create(input(staff(Role.CAREGIVER), "FLOOR", floor, null, Role.CAREGIVER, "PRIMARY")))
                .isInstanceOf(BusinessException.class);
        jdbc.update(
                "UPDATE shift_staff SET absent_at=? WHERE shift_id=?",
                com.cambae.backend.m09.shift.internal.persistence.ShiftStore.sql(from),
                shift);
        assertThatThrownBy(() -> create(usual())).isInstanceOf(BusinessException.class);
        jdbc.update("UPDATE shift_staff SET absent_at=NULL WHERE shift_id=?", shift);
        locations.disable(new LocationRef("ROOM", room));
        assertThatThrownBy(() -> create(input(worker, "ROOM", room, null, Role.CAREGIVER, "PRIMARY")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void FR_026_finalOrOutsideResidentIsRejected() {
        residents.changeStatus(resident, ResidentStatus.DECEASED, time.now());
        assertThatThrownBy(() -> create(input(worker, "RESIDENT", resident, null, Role.CAREGIVER, "PRIMARY")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void FR_028_duplicatePrimaryIsRejectedButSupportIsUnlimited() {
        create(usual());
        assertThatThrownBy(() -> create(usual())).isInstanceOf(BusinessException.class);
        create(input(worker, "FLOOR", floor, null, Role.CAREGIVER, "SUPPORT"));
        create(input(worker, "FLOOR", floor, null, Role.CAREGIVER, "SUPPORT"));
    }

    @Test
    void FR_028_previewAlsoDetectsOverlappingPrimaryInAnotherShift() {
        create(usual());
        long other = staff(Role.CAREGIVER);
        long overlapping = anotherShift(other, from, to);
        assertThatThrownBy(() -> service.preview(
                        overlapping, input(other, "FLOOR", floor, null, Role.CAREGIVER, "PRIMARY"), target()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void FR_037a_newPrimaryCannotOverwriteTheIntervalOfAnEndedAssignment() {
        published();
        long first = create(usual());
        clock.setTo(from.plusSeconds(3600));
        lifecycle.run(time.now(), time.now());
        service.end(first, new Reason("Thay phân công"), target());
        assertThatThrownBy(() -> create(usual())).isInstanceOf(BusinessException.class);
        var cut = jdbc.queryForObject(
                        "SELECT actual_end_at FROM staff_assignment WHERE id=?", LocalDateTime.class, first)
                .toInstant(ZoneOffset.UTC);
        create(new AssignmentInput(worker, "FLOOR", floor, null, Role.CAREGIVER, "PRIMARY", false, cut, to));
    }

    @Test
    void FR_028a_nurseCanDoCaregiverWorkButCaregiverCannotDoNursing() {
        tasks.require(7, new TaskRequirements(Role.NURSING_STAFF, Set.of()));
        assertThatThrownBy(() -> create(input(worker, "FLOOR", floor, 7L, Role.NURSING_STAFF, "PRIMARY")))
                .isInstanceOf(BusinessException.class);
        long nurse = staff(Role.NURSING_STAFF);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, nurse);
        tasks.require(8, new TaskRequirements(Role.CAREGIVER, Set.of()));
        create(input(nurse, "FLOOR", floor, 8L, Role.CAREGIVER, "PRIMARY"));
    }

    @Test
    void FR_030_missingCertificateOrCertificateExpiringDuringIntervalIsRejected() {
        tasks.require(7, new TaskRequirements(Role.CAREGIVER, Set.of("FIRST_AID_TRAINING")));
        assertThatThrownBy(() -> create(input(worker, "FLOOR", floor, 7L, Role.CAREGIVER, "PRIMARY")))
                .isInstanceOf(BusinessException.class);
        credential(worker, time.today());
        var overnight = new AssignmentInput(
                worker, "FLOOR", floor, 7L, Role.CAREGIVER, "PRIMARY", false, from, to.plusSeconds(86400));
        assertThatThrownBy(() -> create(overnight)).isInstanceOf(BusinessException.class);
        create(input(worker, "FLOOR", floor, 7L, Role.CAREGIVER, "PRIMARY"));
    }

    private void credential(long person, LocalDate expires) {
        jdbc.update(
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date) SELECT ?,id,?,? FROM credential_type WHERE code='FIRST_AID_TRAINING'",
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
                com.cambae.backend.m09.shift.internal.persistence.ShiftStore.sql(start),
                com.cambae.backend.m09.shift.internal.persistence.ShiftStore.sql(end));
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", result, person);
        return result;
    }

    @Test
    void SC_005_twoHundredCredentialAndAssignmentChecksNeverAcceptMissingRequirements() {
        credential(worker, time.today());
        long overnight =
                anotherShift(worker, Instant.parse("2026-11-01T16:00:00Z"), Instant.parse("2026-11-02T00:00:00Z"));
        for (int i = 0; i < 200; i++) {
            String required = (i % 3 == 1) ? "MISSING_TRAINING" : "FIRST_AID_TRAINING";
            long task = 10000L + i;
            tasks.require(task, new TaskRequirements(Role.CAREGIVER, Set.of(required)));
            var input = input(worker, "FLOOR", floor, task, Role.CAREGIVER, "PRIMARY");
            long chosen = i % 3 == 2 ? overnight : shift;
            if (i % 3 == 0)
                assertThat(service.preview(chosen, input, target()).warnings()).isEmpty();
            else
                assertThatThrownBy(() -> service.preview(chosen, input, target()))
                        .isInstanceOf(BusinessException.class);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_assignment WHERE shift_id=?", Integer.class, shift))
                .isZero();
    }

    @Test
    void FR_031_unrestrictedPrimaryPreviewWarnsBeforeAnyWrite() {
        tasks.require(7, new TaskRequirements(Role.CAREGIVER, Set.of("FIRST_AID_TRAINING")))
                .plan(resident, Set.of(7L));
        assertThat(service.preview(shift, usual(), target()).warnings()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_assignment WHERE shift_id=?", Integer.class, shift))
                .isZero();
        assertThat(service.create(shift, usual(), target()).warnings()).hasSize(1);
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

    @Test
    void FR_027_cancelRequiresReasonAndCannotBeRepeated() {
        published();
        long id = create(usual());
        assertThatThrownBy(() -> service.cancel(id, null, target())).isInstanceOf(ReasonRequiredException.class);
        service.cancel(id, new Reason("Đổi lịch"), target());
        assertThatThrownBy(() -> service.cancel(id, new Reason("Gửi lại"), target()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void FR_024_floorManagerIntervalsAreExclusiveAndTemporaryReplacementStartsNextDay() {
        long manager = staff(Role.FLOOR_MANAGER), replacement = staff(Role.FLOOR_MANAGER);
        LocalDate today = time.today();
        long id = managers.assign(
                manager, floor, today, null, null, new Target("M09.FLOOR_MANAGER_ASSIGNMENT", null, null, floor));
        assertThatThrownBy(() -> managers.assign(replacement, floor, today, today.plusDays(3), null, target()))
                .isInstanceOf(BusinessException.class);
        managers.end(id, today, new Reason("Nghỉ dài ngày"), target());
        managers.assign(replacement, floor, today.plusDays(1), today.plusDays(3), new Reason("Thay tạm"), target());
        assertThat(duties.assignedFloorManager(floor, time.now())).contains(manager);
        assertThat(duties.assignedFloorManager(floor, time.startOfDay(today.plusDays(1))))
                .contains(replacement);
    }

    @Test
    void BR_M09_17_noShiftDoesNotLockAccountOrEraseFloorManagerHistory() {
        long person = staff(Role.FLOOR_MANAGER);
        managers.assign(person, floor, time.today(), null, null, target());
        assertThat(jdbc.queryForObject("SELECT status FROM account WHERE staff_id=?", String.class, person))
                .isEqualTo("ACTIVE");
        assertThat(assignments.history(
                        person, time.now().minusSeconds(3600), time.now().plusSeconds(3600)))
                .hasSize(1);
    }

    @Test
    void FR_024_twoConcurrentFloorManagerCommandsOnlyOneSucceeds() throws Exception {
        long first = staff(Role.FLOOR_MANAGER), second = staff(Role.FLOOR_MANAGER);
        var barrier = new CyclicBarrier(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> floorRace(first, barrier));
            var b = pool.submit(() -> floorRace(second, barrier));
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            pool.shutdownNow();
        }
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

    @Test
    void FR_026_assignmentRecipientsFollowRoomMovesWithoutRecreatingAssignments() {
        published();
        create(input(worker, "ROOM", room, null, Role.CAREGIVER, "PRIMARY"));
        assertThat(assignments.staffAssignedToResident(resident, from)).contains(worker);
        long newcomer = TestUsers.nextId();
        residents.resident(newcomer, "[Họ tên NCT mới]", null, ResidentStatus.IN_RESIDENCE);
        locations.stay(newcomer, room, from.plusSeconds(60), null);
        assertThat(assignments.staffAssignedToResident(newcomer, from.plusSeconds(60)))
                .contains(worker);
    }

    @Test
    void FR_032_copyRechecksEveryRowAndReturnsSkipReasons() {
        long source = create(usual());
        long absent = staff(Role.CAREGIVER);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, absent);
        long skipped = create(input(absent, "ROOM", room, null, Role.CAREGIVER, "SUPPORT"));
        // Cùng lịch, ca khác thời gian: không tạo lịch tháng trùng phạm vi.
        long sourceShift = shift;
        long template = jdbc.queryForObject("SELECT template_id FROM work_shift WHERE id=?", Long.class, shift);
        shift = insert(
                "INSERT INTO work_shift(schedule_id,template_id,scope_type,scope_id,starts_at,ends_at,requires_handover) VALUES (?,?,'FLOOR',?,?,?,FALSE)",
                schedule,
                template,
                floor,
                com.cambae.backend.m09.shift.internal.persistence.ShiftStore.sql(to),
                com.cambae.backend.m09.shift.internal.persistence.ShiftStore.sql(to.plusSeconds(8 * 3600)));
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, worker);
        var copied = service.copy(sourceShift, shift, target());
        assertThat(copied.copiedIds()).hasSize(1);
        assertThat(copied.notCopied())
                .anyMatch(s -> s.sourceAssignmentId() == skipped && !s.reason().isBlank());
        assertThat(copied.notCopied()).noneMatch(s -> s.sourceAssignmentId() == source);
    }

    private void readyToPublish() {
        long manager = staff(Role.FLOOR_MANAGER);
        managers.assign(manager, floor, time.today(), null, null, target());
        long lead = staff(Role.NURSING_STAFF);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, lead);
        jdbc.update("UPDATE work_shift SET lead_staff_id=? WHERE id=?", lead, shift);
    }

    @Test
    void FR_025_publicationActivatesDraftsAndSnapshotsAllAssignments() {
        long id = create(usual());
        readyToPublish();
        assertThat(schedules.preview(schedule).invalidAssignments()).isEmpty();
        schedules.publish(schedule, List.of(), false, new Reason("Đã kiểm tra lịch tự lập"), target());
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, id))
                .isEqualTo("SCHEDULED");
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_LENGTH(content,'$.assignments') FROM shift_publication WHERE schedule_id=?",
                        Integer.class,
                        schedule))
                .isOne();
    }

    @Test
    void FR_016_invalidDraftCanBeExplicitlyRemovedDuringPublication() {
        long id = create(input(worker, "ROOM", room, null, Role.CAREGIVER, "PRIMARY"));
        readyToPublish();
        locations.disable(new LocationRef("ROOM", room));
        assertThat(schedules.preview(schedule).invalidAssignments()).anyMatch(a -> a.assignmentId() == id);
        assertThatThrownBy(() -> schedules.publish(schedule, List.of(), false, new Reason("Kiểm tra"), target()))
                .isInstanceOf(BusinessException.class);
        schedules.publish(schedule, List.of(id), false, new Reason("Gỡ phân công ở phòng ngừng dùng"), target());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_assignment WHERE id=?", Integer.class, id))
                .isZero();
    }

    @Test
    void FR_025_removingDraftMemberAlsoDeletesTheirDraftAssignments() {
        create(usual());
        long template = jdbc.queryForObject("SELECT template_id FROM work_shift WHERE id=?", Long.class, shift);
        var input = new com.cambae.backend.m09.shift.internal.domain.ShiftInput(
                template, time.today(), from, to, List.of(), null, false, null);
        schedules.editShift(shift, "\"0\"", input, target());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_assignment WHERE shift_id=?", Integer.class, shift))
                .isZero();
    }

    @Test
    void FR_025_deletingDraftShiftAlsoDeletesItsDraftAssignments() {
        create(usual());
        schedules.deleteShift(shift, "\"0\"", target());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_assignment WHERE shift_id=?", Integer.class, shift))
                .isZero();
    }

    @Test
    void FR_028_CareSpiReturnsOnlyTheMostSpecificPrimaryForEachRole() {
        published();
        create(usual());
        long specific = staff(Role.CAREGIVER);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, specific);
        create(input(specific, "RESIDENT", resident, null, Role.CAREGIVER, "PRIMARY"));
        assertThat(care.assignments(resident, shift))
                .extracting(a -> a.staffId())
                .containsExactly(specific);
        assertThat(nursing.assignedTo(resident, from)).isEmpty();
        assertThat(alerts.assignedNurse(resident, from)).isEmpty();
        assertThat(alerts.assignmentsInRange(resident, from, to)).anyMatch(a -> a.staffId() == specific);
    }

    @Test
    void BR_M09_16_locationReferencesIncludeDraftAssignmentsAndCurrentFloorManager() {
        long id = create(input(worker, "ROOM", room, null, Role.CAREGIVER, "PRIMARY"));
        // Bản thật, không lấy FakeLocationReferences của nguồn 003.
        assertThat(locationReferences.activeReferences(new LocationRef("ROOM", room)))
                .anyMatch(r -> r.id() == id && r.type().equals("M09.ASSIGNMENT"));
        long manager = staff(Role.FLOOR_MANAGER);
        long assigned = managers.assign(manager, floor, time.today(), null, null, target());
        assertThat(locationReferences.activeReferences(new LocationRef("FLOOR", floor)))
                .anyMatch(r -> r.id() == assigned && r.type().equals("M09.FLOOR_MANAGER_ASSIGNMENT"));
    }

    @Autowired
    @Qualifier("shiftLocationReferences")
    com.cambae.backend.m03.api.spi.LocationReferences locationReferences;

    @Test
    void FR_029_remindsOnceOnPublicationAndOnceAtShiftStart() {
        readyToPublish();
        schedules.publish(schedule, List.of(), false, new Reason("Kiểm tra người chưa phân công"), target());
        assertThat(applicationEvents.stream(com.cambae.backend.m09.api.events.ResidentsWithoutAssignedStaff.class)
                        .filter(e -> e.shiftId() == shift))
                .hasSize(1);
        clock.setTo(from.plusSeconds(60));
        jobs.runDue(com.cambae.backend.m09.internal.service.M09TimedTask.Cadence.MINUTE);
        jobs.runDue(com.cambae.backend.m09.internal.service.M09TimedTask.Cadence.MINUTE);
        assertThat(applicationEvents.stream(com.cambae.backend.m09.api.events.ResidentsWithoutAssignedStaff.class)
                        .filter(e -> e.shiftId() == shift))
                .hasSize(2);
    }

    @Test
    void FR_024_missingFloorManagerRemindsManagersAtEachShiftStart() {
        published();
        clock.setTo(from.plusSeconds(60));
        jobs.runDue(com.cambae.backend.m09.internal.service.M09TimedTask.Cadence.MINUTE);
        assertThat(applicationEvents.stream(com.cambae.backend.m09.api.events.FloorManagerMissing.class)
                        .filter(e -> e.floorAreaId() == floor))
                .hasSize(1);
    }

    @Test
    void FR_026_finalStatusEndsDirectAssignmentsAndPreservesTheirHistoricalScope() {
        published();
        long id = create(input(worker, "RESIDENT", resident, null, Role.CAREGIVER, "PRIMARY"));
        clock.setTo(from.plusSeconds(60));
        lifecycle.run(time.now(), time.now());
        residents.changeStatus(resident, ResidentStatus.DECEASED, time.now());
        Instant cut = time.now();
        new org.springframework.transaction.support.TransactionTemplate(transactions)
                .executeWithoutResult(
                        tx -> publisher.publishEvent(new com.cambae.backend.m01.api.events.ResidentStatusChanged(
                                resident,
                                ResidentStatus.IN_RESIDENCE,
                                ResidentStatus.DECEASED,
                                StatusCommand.RECORD_DEATH,
                                TestUsers.nextId(),
                                cut)));
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, id))
                .isEqualTo("ENDED");
        assertThat(tasks.endedAssignments()).contains(id);
        assertThat(assignments.activeAt(worker, time.now())).isEmpty();
        assertThat(assignments.history(worker, from, to)).anyMatch(a -> a.scopeEndsAt() != null);
    }

    @Test
    void BR_M15_05_temporaryLoginLockDoesNotCancelTheEmployeesEligibilityForAssignments() {
        jdbc.update(
                "UPDATE account SET status='TEMPORARILY_LOCKED',locked_until=? WHERE staff_id=?",
                com.cambae.backend.m09.shift.internal.persistence.ShiftStore.sql(
                        time.now().plusSeconds(300)),
                worker);
        create(usual());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void FR_030_assignedNurseUsesTheConfiguredMedicationLicenseAndFeedsAllThreeSpis() {
        jdbc.update(
                "UPDATE professional_activity SET required_license_type='NURSING_LICENSE' WHERE code='MEDICATION_ADMINISTRATION'");
        long nurse = staff(Role.NURSING_STAFF);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, nurse);
        var input = new AssignmentInput(nurse, "FLOOR", floor, null, Role.NURSING_STAFF, "PRIMARY", true, null, null);
        assertThatThrownBy(() -> create(input)).isInstanceOf(BusinessException.class);
        jdbc.update(
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date) SELECT ?,id,?,? FROM credential_type WHERE code='NURSING_LICENSE'",
                nurse,
                time.today().minusDays(1),
                time.today().plusDays(1));
        published();
        create(input);
        assertThat(care.assignedNurses(resident, from)).containsExactly(nurse);
        assertThat(nursing.assignedTo(resident, from)).contains(nurse);
        assertThat(alerts.assignedNurse(resident, from)).contains(nurse);
    }

    @Test
    void FR_024_floorManagerAbsenceAndSubstitutionAreEvaluatedAtTheCurrentTime() {
        long manager = staff(Role.FLOOR_MANAGER);
        LocalDate day = time.today();
        managers.assign(manager, floor, day, day, null, target());
        Instant start = time.startOfDay(day).plusSeconds(23 * 3600), end = start.plusSeconds(8 * 3600);
        long lead = staff(Role.NURSING_STAFF);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, lead);
        jdbc.update(
                "UPDATE work_shift SET starts_at=?,ends_at=?,lead_staff_id=? WHERE id=?",
                com.cambae.backend.m09.shift.internal.persistence.ShiftStore.sql(start),
                com.cambae.backend.m09.shift.internal.persistence.ShiftStore.sql(end),
                lead,
                shift);
        published();
        clock.setTo(start.plusSeconds(2 * 3600));
        assertThat(duties.assignedFloorManager(floor, time.now())).isEmpty();
        assertThat(duties.floorManagerSubstitute(floor, time.now())).contains(lead);
        assertThat(duties.shiftHasNoFloorManager(shift)).isTrue();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Users {
        @Bean
        @Primary
        SwitchableUser current() {
            return new SwitchableUser();
        }
    }
}
