package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.admission.api.dto.AttendanceScheduleView;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.*;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.resident.api.events.CareLevelChanged;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.staffing.api.events.*;
import com.eldercare.backend.staffing.ratio.internal.service.StaffingRatioService;
import com.eldercare.backend.staffing.shift.internal.domain.*;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import com.eldercare.backend.staffing.shift.internal.service.ShiftScheduleService;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.administration.*;
import com.eldercare.backend.support.fake.admission.*;
import com.eldercare.backend.support.fake.roombedallocation.*;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
@Import({
    FakeM02Config.class,
    FakeLocations.Config.class,
    FakeM15Config.class,
    com.eldercare.backend.support.fake.care.FakeM04Config.class,
    com.eldercare.backend.support.fake.resident.FakeCareLevelChangeRequests.class,
    StaffingRatioIT.Users.class
})
class StaffingRatioIT extends MySqlIntegrationTestBase {
    private static final java.util.concurrent.atomic.AtomicInteger CASES =
            new java.util.concurrent.atomic.AtomicInteger();

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    SwitchableUser current;

    @Autowired
    FakeLocations locations;

    @Autowired
    FakeContractSchedules attendance;

    @Autowired
    FakeAssignments duties;

    @Autowired
    ShiftScheduleService schedules;

    @Autowired
    StaffingRatioService ratios;

    @Autowired
    com.eldercare.backend.support.fake.resident.FakeCareLevelChangeRequests levelRequests;

    @Autowired
    com.eldercare.backend.staffing.staff.internal.service.StaffEmploymentEffects employment;

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    TransactionTemplate tx;

    TestUsers users;
    TestUsers.TestUser manager;
    String bearer;
    long floor, room, template, schedule;
    Instant from, to;
    Instant clockBase;
    List<Long> members;

    @BeforeEach
    void setup() throws Exception {
        clockBase = Instant.parse("2026-10-24T00:00:00Z").plusSeconds(60L * CASES.incrementAndGet());
        clock.setTo(clockBase);
        users = new TestUsers(jdbc, time);
        manager = user(Role.NURSING_HOME_MANAGER);
        current.set(manager.user());
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Quản lý kiểm thử tỷ lệ]','Quản lý viện',?)",
                manager.staffId(),
                manager.accountId());
        bearer = TestUsers.bearer(users.session(manager.accountId()));
        floor = TestUsers.nextId();
        room = TestUsers.nextId();
        locations.floor(floor, null).room(room, floor).daycareRestLocation(null);
        attendance.clear();
        from = Instant.parse("2027-11-01T00:00:00Z");
        to = from.plus(Duration.ofHours(8));
        members = List.of(
                staff(Role.NURSING_STAFF), staff(Role.CAREGIVER), staff(Role.CAREGIVER), staff(Role.NURSING_STAFF));
        template = schedules
                .createTemplate(new TemplateInput(
                        "[Mẫu tỷ lệ tích hợp]", LocalTime.of(7, 0), LocalTime.of(15, 0), "FLOOR", false, true))
                .id();
        schedule = schedules
                .createSchedule(YearMonth.of(2027, 11), "FLOOR", floor, new Target("M09.SCHEDULE", null, null, floor))
                .id();
        duties.assign(
                manager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.startOfDay(LocalDate.of(2027, 11, 1)),
                        time.startOfDay(LocalDate.of(2027, 12, 1))));
        parameter("CFG-M09-01", Map.of(Long.toString(template), 8));
        parameter("CFG-M09-12", Map.of("BASIC", 1, "REGULAR", 1.5, "SPECIAL", 2.5));
    }

    @AfterEach
    void reset() throws Exception {
        clock.setTo(clockBase.plusSeconds(30));
        bearer = TestUsers.bearer(users.session(manager.accountId()));
        current.set(manager.user());
        attendance.available(true);
        restore("CFG-M09-01");
        restore("CFG-M09-12");
        clock.reset();
    }

    private void parameter(String code, Object value) throws Exception {
        changeParameter(code, Map.of("value", value, "reason", "Cấu hình tỷ lệ cho kiểm thử"));
    }

    private void restore(String code) throws Exception {
        changeParameter(code, Map.of("restoreDefault", true, "reason", "Khôi phục tham số sau kiểm thử"));
    }

    private void changeParameter(String code, Object body) throws Exception {
        clock.advanceBy(Duration.ofMillis(1));
        long version = jdbc.queryForObject("SELECT version FROM parameter WHERE code=?", Long.class, code);
        mvc.perform(post("/api/v1/parameters/{code}/change-value", code)
                        .header("Authorization", bearer)
                        .header("If-Match", "\"" + version + "\"")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    private long staff(Role... roles) {
        var u = user(roles);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Nhân viên tỷ lệ]','Chăm sóc',?)",
                u.staffId(),
                u.accountId());
        return u.staffId();
    }

    private TestUsers.TestUser user(Role... roles) {
        // Các test import/tạo hồ sơ trước đó có thể đã dùng AUTO_INCREMENT vượt bộ đếm TestUsers.
        long used = jdbc.queryForObject("SELECT COALESCE(MAX(id),0) FROM staff_profile", Long.class);
        while (TestUsers.nextId() <= used) {
            /* Dành id chưa có hồ sơ, giữ đúng liên kết tài khoản. */
        }
        return users.staff(roles);
    }

    private long resident(CareLevel level) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,profile_code,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES (?,?,'[Họ tên NCT tỷ lệ]','1940-01-01','FEMALE','IN_RESIDENCE',?,?,?,?)",
                id,
                "RATIO-" + id,
                ShiftStore.sql(time.now()),
                level.name(),
                ShiftStore.sql(time.now()),
                manager.accountId());
        return id;
    }

    private long inpatient(CareLevel level) {
        long id = resident(level);
        locations.stay(id, room, from.minusSeconds(86400), null);
        return id;
    }

    private void history(long resident, String before, String next, String command, Instant at) {
        jdbc.update(
                "INSERT INTO resident_status_history(resident_id,from_status,to_status,command,occurred_at,performed_by,by_system,reason) VALUES (?,?,?,?,?,?,TRUE,'Mốc trạng thái kiểm thử')",
                resident,
                before,
                next,
                command,
                ShiftStore.sql(at),
                manager.accountId());
    }

    private long save() {
        return schedules
                .addShift(
                        schedule,
                        "\"0\"",
                        new ShiftInput(
                                template,
                                LocalDate.of(2027, 11, 1),
                                from,
                                to,
                                members.stream()
                                        .map(id -> new ShiftInput.Member(id, false))
                                        .toList(),
                                null,
                                false,
                                null),
                        Target.UNSPECIFIED)
                .shiftId();
    }

    @Test
    void BR_M09_02_realDraftSaveStores35Weight_previewDoesNotWrite_publishDoesNotBlockForRatio() {
        for (int i = 0; i < 20; i++) inpatient(CareLevel.BASIC);
        for (int i = 0; i < 6; i++) inpatient(CareLevel.SPECIAL);
        long id = save();
        var result = ratios.latest(id).orElseThrow();
        assertThat(result.totalWeight()).isEqualByComparingTo("35");
        assertThat(result.ratio()).isEqualByComparingTo("8.75");
        assertThat(schedules.preview(schedule).warnings())
                .anyMatch(w -> w.code().equals("BELOW_STAFFING_THRESHOLD"));
        assertThat(ratios.history(id)).hasSize(1);
        schedules.publish(
                schedule, List.of(), false, new Reason("Đã xem tỷ lệ và công bố lịch tự lập"), Target.UNSPECIFIED);
        assertThat(ratios.history(id)).hasSize(2);
        assertThat(ratios.latest(id).orElseThrow().event()).isEqualTo("PUBLISHED");
        assertThat(jdbc.queryForObject("SELECT below_threshold FROM work_shift WHERE id=?", Boolean.class, id))
                .isTrue();
    }

    @Test
    void FR_034_absenceCoveringWholeShiftExcluded_partialAbsenceAndTouchingEdgesStillCount() {
        long absent = inpatient(CareLevel.SPECIAL), partial = inpatient(CareLevel.SPECIAL);
        history(absent, "IN_RESIDENCE", "TEMPORARILY_ABSENT", "GRANT_TEMPORARY_ABSENCE", from.minusSeconds(1));
        history(partial, "IN_RESIDENCE", "TEMPORARILY_ABSENT", "GRANT_TEMPORARY_ABSENCE", from.minusSeconds(1));
        history(partial, "TEMPORARILY_ABSENT", "IN_RESIDENCE", "RECORD_RETURN", from.plusSeconds(3600));
        long id = save();
        assertThat(ratios.latest(id).orElseThrow().totalWeight()).isEqualByComparingTo("2.5");
    }

    @Test
    void FR_034_daycareCountsOnlyWhenAttendanceOverlaps_andUsesRestAreaLocation() {
        long resident = resident(CareLevel.SPECIAL);
        locations.daycareRestLocation(new LocationRef("FLOOR", floor));
        attendance.schedule(
                resident,
                new AttendanceScheduleView(LocalDate.of(2027, 11, 1), LocalTime.of(7, 30), LocalTime.of(16, 30)));
        long id = save();
        assertThat(ratios.latest(id).orElseThrow().totalWeight()).isEqualByComparingTo("2.5");
        locations.daycareRestLocation(new LocationRef("FLOOR", TestUsers.nextId()));
        ratios.recalculate(id, "ALLOCATION_CHANGED");
        assertThat(ratios.latest(id).orElseThrow().totalWeight()).isEqualByComparingTo("0");
    }

    @Test
    void BR_M09_14_membersWithoutAssignmentsStillCount_doubleRoleOnce_excludesFloorOnlyAndApprovedLeave() {
        members = List.of(
                staff(Role.NURSING_STAFF, Role.CAREGIVER), staff(Role.FLOOR_MANAGER), staff(Role.NURSING_STAFF));
        inpatient(CareLevel.BASIC);
        long id = save();
        assertThat(ratios.latest(id).orElseThrow().staffCount()).isEqualTo(2);
        jdbc.update(
                "UPDATE shift_staff SET approved_leave_request_id=123 WHERE shift_id=? AND staff_id=?",
                id,
                members.getLast());
        ratios.recalculate(id, "SHIFT_SWAP_015");
        assertThat(ratios.latest(id).orElseThrow().staffCount()).isEqualTo(1);
    }

    @Test
    void FR_034_pendingFutureCareLevelDoesNotReplaceCurrentLevel() {
        long resident = inpatient(CareLevel.BASIC);
        levelRequests.open(resident, 77);
        assertThat(levelRequests.hasOpenRequest(resident)).isTrue();
        long id = save();
        assertThat(ratios.latest(id).orElseThrow().totalWeight()).isEqualByComparingTo("1");
    }

    @Test
    void FR_035_employmentWithdrawalRecalculatesInSameTransaction_andRollsBackWithCommand() {
        inpatient(CareLevel.BASIC);
        long id = save();
        tx.executeWithoutResult(t -> {
            employment.apply(members.getFirst(), time.now());
            assertThat(ratios.latest(id).orElseThrow().staffCount()).isEqualTo(3);
            t.setRollbackOnly();
        });
        assertThat(ratios.history(id)).hasSize(1);
    }

    @Test
    void FR_035_careLevelEventRecalculatesOnlyAfterCommit_withAppliedCurrentLevel() {
        long resident = inpatient(CareLevel.BASIC);
        long id = save();
        tx.executeWithoutResult(t -> {
            jdbc.update("UPDATE resident SET current_care_level='SPECIAL' WHERE id=?", resident);
            events.publishEvent(new CareLevelChanged(
                    resident,
                    Optional.of(CareLevel.BASIC),
                    CareLevel.SPECIAL,
                    time.now(),
                    CareLevelBasisType.RESIDENCY_CHANGE_REQUEST,
                    88));
            assertThat(ratios.history(id)).hasSize(1);
        });
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(ratios.latest(id).orElseThrow().totalWeight())
                        .isEqualByComparingTo("2.5"));
    }

    @Test
    void FR_034_missingConfigurationOrSourceReportsUnavailable_withoutInventingZero() throws Exception {
        restore("CFG-M09-12");
        long id = save();
        assertThat(ratios.latest(id)).isEmpty();
        assertThat(schedules.preview(schedule).warnings())
                .anyMatch(w -> w.code().equals("STAFFING_RATIO_UNAVAILABLE"));
        parameter("CFG-M09-12", Map.of("BASIC", 1, "REGULAR", 1.5, "SPECIAL", 2.5));
        attendance.available(false);
        assertThat(ratios.recalculate(id, "CARE_LEVEL_CHANGED"))
                .anyMatch(w -> w.code().equals("STAFFING_RATIO_UNAVAILABLE"));
        assertThat(ratios.latest(id)).isEmpty();
    }

    @Test
    void FR_034_overnightShiftReadsBothVietnamDates_andExcludesAttendanceTouchingEnd() {
        from = Instant.parse("2027-11-01T16:00:00Z");
        to = from.plusSeconds(28800);
        long first = resident(CareLevel.SPECIAL), touching = resident(CareLevel.BASIC);
        locations.daycareRestLocation(new LocationRef("FLOOR", floor));
        attendance.schedule(
                first,
                new AttendanceScheduleView(LocalDate.of(2027, 11, 1), LocalTime.of(7, 30), LocalTime.of(16, 30)));
        attendance.schedule(
                touching,
                new AttendanceScheduleView(LocalDate.of(2027, 11, 2), LocalTime.of(7, 0), LocalTime.of(16, 30)));
        long id = save();
        assertThat(ratios.latest(id).orElseThrow().totalWeight()).isEqualByComparingTo("0");
        attendance.schedule(
                first, new AttendanceScheduleView(LocalDate.of(2027, 11, 2), LocalTime.of(6, 30), LocalTime.of(7, 0)));
        ratios.recalculate(id, "ALLOCATION_CHANGED");
        assertThat(ratios.latest(id).orElseThrow().totalWeight()).isEqualByComparingTo("2.5");
    }

    @Test
    void FR_035_bedTransferRecalculatesOldAndNewFloorAfterCommit() throws Exception {
        long resident = inpatient(CareLevel.BASIC),
                old = save(),
                nextFloor = TestUsers.nextId(),
                nextRoom = TestUsers.nextId();
        locations.floor(nextFloor, null).room(nextRoom, nextFloor);
        long nextSchedule = schedules
                .createSchedule(
                        YearMonth.of(2027, 11), "FLOOR", nextFloor, new Target("M09.SCHEDULE", null, null, nextFloor))
                .id();
        long next = schedules
                .addShift(
                        nextSchedule,
                        "\"0\"",
                        new ShiftInput(
                                template,
                                LocalDate.of(2027, 11, 1),
                                from,
                                to,
                                List.of(new ShiftInput.Member(staff(Role.CAREGIVER), false)),
                                null,
                                false,
                                null),
                        Target.UNSPECIFIED)
                .shiftId();
        clock.setTo(from.minusSeconds(3600));
        Instant at = time.now();
        locations.closeStays(resident, at).stay(resident, nextRoom, at, null);
        tx.executeWithoutResult(
                t -> events.publishEvent(new com.eldercare.backend.roombedallocation.api.events.BedTransferred(
                        resident, 1, 2, floor, nextFloor, at)));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(ratios.latest(old).orElseThrow().totalWeight()).isEqualByComparingTo("0");
            assertThat(ratios.latest(next).orElseThrow().totalWeight()).isEqualByComparingTo("1");
        });
    }

    @Test
    void FR_035_allocationStartedAndClosedUseHistoryIncludingTheClosingBoundary() {
        long resident = resident(CareLevel.BASIC), id = save();
        clock.setTo(from.minusSeconds(3600));
        locations.stay(resident, room, time.now(), null);
        tx.executeWithoutResult(
                t -> events.publishEvent(new com.eldercare.backend.roombedallocation.api.events.BedAllocationStarted(
                        1, resident, 7, time.now())));
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(ratios.latest(id).orElseThrow().totalWeight())
                        .isEqualByComparingTo("1"));
        clock.advanceBy(Duration.ofMinutes(1));
        Instant closedAt = time.now();
        locations.closeStays(resident, closedAt);
        tx.executeWithoutResult(
                t -> events.publishEvent(new com.eldercare.backend.roombedallocation.api.events.BedAllocationClosed(
                        1, resident, 7, closedAt, "Đóng phân bổ")));
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(ratios.latest(id).orElseThrow().totalWeight())
                        .isEqualByComparingTo("0"));
    }

    @Test
    void FR_035_finalStatusDuringShiftRemovesWeightForRemainingPart_withoutRewritingOldCalculation() {
        long resident = inpatient(CareLevel.SPECIAL), id = save();
        clock.setTo(from.plusSeconds(3600));
        Instant at = time.now();
        tx.executeWithoutResult(t -> {
            jdbc.update(
                    "UPDATE resident SET status='RESIDENCY_ENDED',status_since=? WHERE id=?",
                    ShiftStore.sql(at),
                    resident);
            history(resident, "IN_RESIDENCE", "RESIDENCY_ENDED", "END_RESIDENCY", at);
            events.publishEvent(new com.eldercare.backend.resident.api.events.ResidentStatusChanged(
                    resident,
                    ResidentStatus.IN_RESIDENCE,
                    ResidentStatus.RESIDENCY_ENDED,
                    StatusCommand.END_RESIDENCY,
                    1,
                    at));
        });
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(ratios.latest(id).orElseThrow().totalWeight())
                        .isEqualByComparingTo("0"));
        assertThat(ratios.history(id).getFirst().totalWeight()).isEqualByComparingTo("2.5");
    }

    @Test
    void FR_035_recalculationAfterWaitingUsesCurrentMembership_notOldTransactionSnapshot() throws Exception {
        inpatient(CareLevel.BASIC);
        long id = save();
        var snapshotReady = new java.util.concurrent.CountDownLatch(1);
        var changed = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var pending = executor.submit(() -> tx.execute(t -> {
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM shift_staff WHERE shift_id=? AND withdrawn_on_resignation_at IS NULL",
                        Integer.class,
                        id);
                snapshotReady.countDown();
                try {
                    if (!changed.await(10, java.util.concurrent.TimeUnit.SECONDS))
                        throw new IllegalStateException("Chờ transaction cập nhật");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ratios.recalculate(id, "EMPLOYMENT_ENDED");
                return true;
            }));
            assertThat(snapshotReady.await(5, java.util.concurrent.TimeUnit.SECONDS))
                    .isTrue();
            tx.executeWithoutResult(t -> employment.apply(members.getFirst(), time.now()));
            changed.countDown();
            pending.get(15, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(ratios.latest(id).orElseThrow().staffCount()).isEqualTo(3);
        } finally {
            changed.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void FR_034_leavingHalfwayThroughShiftKeepsFullWeight_evenAfterTheAbsenceStarts() {
        long resident = inpatient(CareLevel.SPECIAL), id = save();
        clock.setTo(from.plusSeconds(3600));
        Instant at = time.now();
        tx.executeWithoutResult(t -> {
            jdbc.update(
                    "UPDATE resident SET status='TEMPORARILY_ABSENT',status_since=? WHERE id=?",
                    ShiftStore.sql(at),
                    resident);
            history(resident, "IN_RESIDENCE", "TEMPORARILY_ABSENT", "GRANT_TEMPORARY_ABSENCE", at);
            events.publishEvent(new com.eldercare.backend.resident.api.events.ResidentStatusChanged(
                    resident,
                    ResidentStatus.IN_RESIDENCE,
                    ResidentStatus.TEMPORARILY_ABSENT,
                    StatusCommand.GRANT_TEMPORARY_ABSENCE,
                    1,
                    at));
        });
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(ratios.history(id)).hasSize(2));
        assertThat(ratios.latest(id).orElseThrow().totalWeight()).isEqualByComparingTo("2.5");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Users {
        @Bean
        @Primary
        SwitchableUser ratioUser() {
            return new SwitchableUser();
        }
    }
}
