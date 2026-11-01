package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.care.api.*;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.api.spi.DayCareCalendarPort.CalendarView;
import com.eldercare.backend.care.api.spi.DayCarePickupPort.PickupView;
import com.eldercare.backend.care.internal.scheduling.service.*;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.admission.*;
import com.eldercare.backend.support.fake.care.*;
import com.eldercare.backend.support.fake.resident.*;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
@WithMockUser
@Transactional
@RecordApplicationEvents
@Import({
    FR_028a_ReasonCatalogHttpIT.Users.class,
    FakeM01Config.class,
    FakeM02Config.class,
    FakeM04Config.class,
    BR_M04_02_DayCareAttendanceIT.Locations.class
})
class BR_M04_02_DayCareAttendanceIT extends MySqlIntegrationTestBase {
    @TestConfiguration
    static class Locations {
        @Bean
        @Primary
        FakeLocations attendanceLocations() {
            return new FakeLocations();
        }
    }

    @Autowired
    org.springframework.context.ApplicationContext context;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    FakeResidents residents;

    @Autowired
    FakeContracts contracts;

    @Autowired
    FakeContractSchedules schedules;

    @Autowired
    FakeDayCareCalendar calendar;

    @Autowired
    FakeDayCarePickups pickups;

    @Autowired
    FakeLocations locations;

    @Autowired
    JsonMapper json;

    @Autowired
    ApplicationEvents events;

    long resident, floor;
    Instant at;
    LocalDate date;

    @BeforeEach
    void setup() {
        for (long old : residents.byStatus(Set.of(ResidentStatus.IN_RESIDENCE)))
            residents.resident(old, "[Hồ sơ test cũ]", Gender.MALE, ResidentStatus.RESIDENCY_ENDED);
        resident = TestUsers.nextId();
        floor = TestUsers.nextId();
        date = time.today();
        at = time.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        user.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        residents.resident(resident, "[Người cao tuổi kiểm thử]", Gender.MALE, ResidentStatus.IN_RESIDENCE);
        contracts.period(
                resident, new ResidenceTypePeriod(ResidenceType.DAY_CARE, date.minusDays(2), Optional.empty()));
        schedules
                .clear()
                .schedule(
                        resident,
                        new com.eldercare.backend.admission.api.dto.AttendanceScheduleView(
                                date, LocalTime.of(8, 0), LocalTime.of(17, 0)));
        calendar.reset();
        calendar.day(resident, date, new CalendarView(false, null, null));
        pickups.reset();
        locations.floor(floor, null).daycareRestLocation(new LocationRef("FLOOR", floor));
    }

    private org.springframework.test.web.servlet.ResultActions arrive() throws Exception {
        return mvc.perform(post("/api/v1/day-care-attendances/check-in")
                .header("Idempotency-Key", "arrival-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"residentId\":" + resident + ",\"at\":\"" + at + "\"}"));
    }

    private long arriveId() throws Exception {
        return json.readTree(arrive().andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("id")
                .asLong();
    }

    private DayCareAttendanceApi api() {
        return context.getBean(DayCareAttendanceApi.class);
    }

    private org.springframework.test.web.servlet.ResultActions leave(long id, long pickup, Instant when)
            throws Exception {
        return mvc.perform(post("/api/v1/day-care-attendances/" + id + "/check-out")
                .header("Idempotency-Key", "leave-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pickupRecordId\":" + pickup + ",\"at\":\"" + when + "\"}"));
    }

    @Test
    void BR_M04_02_arrivalPersistsConfirmedRecordAndPublishesPlanningAndAttendance() throws Exception {
        long id = arriveId();
        assertThat(api().onDate(resident, date).orElseThrow().status()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(api().onDate(resident, date).orElseThrow().id()).isEqualTo(id);
        assertThat(events.stream(DayCareCheckedIn.class)
                        .filter(e -> e.residentId() == resident)
                        .count())
                .isEqualTo(1);
        assertThat(events.stream(TaskPlanningNeeded.class)
                        .filter(e -> e.residentId() == resident)
                        .count())
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M04.ATTENDANCE_RECORD' AND entity_type='M01.RESIDENT' AND entity_id=? AND confirmed_at IS NOT NULL",
                        Integer.class,
                        resident))
                .isEqualTo(1);
    }

    @Test
    void FR_013_secondArrivalIsRejectedWithoutASecondRecord() throws Exception {
        arriveId();
        arrive().andExpect(status().isConflict());
    }

    @Test
    void FR_013_holidayArrivalIsAnAdHocSessionWithNoExpectedTimes() throws Exception {
        calendar.day(resident, date, new CalendarView(true, null, null));
        arrive().andExpect(status().isOk())
                .andExpect(jsonPath("$.adHocSession").value(true))
                .andExpect(jsonPath("$.expectedArrival").isEmpty());
    }

    @Test
    void FR_013_timelyAbsenceBlocksArrivalButLateNoticeDoesNotChangeState() throws Exception {
        calendar.day(resident, date, new CalendarView(false, TestUsers.nextId(), null));
        api().refreshCalendar(resident, date);
        arrive().andExpect(status().isConflict());
        assertThat(api().onDate(resident, date).orElseThrow().status()).isEqualTo(AttendanceStatus.REPORTED_ABSENT);
    }

    @Test
    void FR_014_lateNoticeIsRetainedWithoutChangingPresent() throws Exception {
        arriveId();
        calendar.day(resident, date, new CalendarView(false, null, TestUsers.nextId()));
        api().refreshCalendar(resident, date);
        mvc.perform(get("/api/v1/day-care-attendances").param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PRESENT"))
                .andExpect(jsonPath("$[0].lateAbsenceReported").value(true));
    }

    @Test
    void BR_M04_02_onlyValidPickupCanCheckOutAndItsRecordIsMarkedUsed() throws Exception {
        long id = arriveId(), pickup = TestUsers.nextId();
        leave(id, pickup, at.plusSeconds(60)).andExpect(status().isConflict());
        pickups.valid(pickup, resident, at.plusSeconds(60), new PickupView(pickup, "SELF_RETURN"));
        leave(id, pickup, at.plusSeconds(60))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendance.checkoutType").value("SELF_RETURN"));
        assertThat(pickups.used()).containsKey(pickup);
        assertThat(api().onDate(resident, date).orElseThrow().status()).isEqualTo(AttendanceStatus.LEFT);
    }

    @Test
    void FR_018a_returnTimeChangeAndRestoreRequireTheMatchingRegistration() throws Exception {
        arriveId();
        long first = TestUsers.nextId(), second = TestUsers.nextId();
        api().changeReturnTime(resident, date, LocalTime.of(18, 0), first);
        api().changeReturnTime(resident, date, LocalTime.of(19, 0), second);
        api().restoreReturnTime(resident, date, first);
        assertThat(api().returnTimeOn(resident, date)).contains(LocalTime.of(19, 0));
        api().restoreReturnTime(resident, date, second);
        assertThat(api().returnTimeOn(resident, date)).contains(LocalTime.of(17, 0));
    }

    @Test
    void FR_013_calendarHolidayCancelsOnlyAnUnarrivedSession() throws Exception {
        api().refreshCalendar(resident, date);
        calendar.day(resident, date, new CalendarView(true, null, null));
        api().refreshCalendar(resident, date);
        assertThat(api().onDate(resident, date).orElseThrow().status()).isEqualTo(AttendanceStatus.HOLIDAY_CANCELLED);
        arriveId();
        api().refreshCalendar(resident, date);
        assertThat(api().onDate(resident, date).orElseThrow().status()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    void BR_M04_02_residentMustBeStayingAndHaveDayCareResidence() throws Exception {
        residents.resident(resident, "[Người kiểm thử vắng]", Gender.MALE, ResidentStatus.TEMPORARILY_ABSENT);
        arrive().andExpect(status().isConflict());
    }

    @Test
    void FR_049_doctorCannotCheckIn() throws Exception {
        user.set(new TestUsers(jdbc, time).staff(Role.DOCTOR).user());
        arrive().andExpect(status().isForbidden());
    }

    private long task(long owner, Instant planned, String state) {
        long id = TestUsers.nextId();
        String data = json.writeValueAsString(new com.eldercare.backend.care.internal.task.domain.CareTaskData(
                1, "Loại test", "Công việc test", "PERFORMED", false, null, Set.of(), null, null, true));
        jdbc.update(
                "INSERT INTO care_task(id,resident_id,task_type_id,basis_type,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,status,data,created_by,created_at) VALUES(?,?,1,'AD_HOC',?,?, ?,?,?,'NORMAL','CAREGIVER',?,?,?,?)",
                id,
                owner,
                TestUsers.nextId(),
                floor,
                LocalDateTime.ofInstant(planned, BusinessTime.ZONE),
                LocalDateTime.ofInstant(planned, BusinessTime.ZONE),
                LocalDateTime.ofInstant(planned.plusSeconds(600), BusinessTime.ZONE),
                state,
                data,
                user.accountId(),
                LocalDateTime.ofInstant(at, BusinessTime.ZONE));
        return id;
    }

    @Test
    void FR_013_holidayDeclaredAfterArrivalDoesNotEraseTheAttendedSessionTimes() throws Exception {
        arriveId();
        calendar.day(resident, date, new CalendarView(true, null, null));
        api().refreshCalendar(resident, date);
        var current = api().onDate(resident, date).orElseThrow();
        assertThat(current.status()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(current.expectedArrival()).contains(LocalTime.of(8, 0));
        assertThat(current.currentExpectedReturn()).contains(LocalTime.of(17, 0));
        assertThat(current.adHocSession()).isFalse();
    }

    @Test
    void FR_019_checkoutCancelsOnlyThisResidentsFutureNotYetDueTasks() throws Exception {
        long id = arriveId(), pickup = TestUsers.nextId();
        Instant departure = at.plusSeconds(60);
        long future = task(resident, at.plusSeconds(3600), "NOT_YET_DUE"),
                earlier = task(resident, at.plusSeconds(30), "NOT_YET_DUE"),
                due = task(resident, at.minusSeconds(30), "DUE"),
                other = task(TestUsers.nextId(), at.plusSeconds(3600), "NOT_YET_DUE");
        pickups.valid(pickup, resident, departure, new PickupView(pickup, "PICKUP"));
        leave(id, pickup, departure)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledTaskIds[0]").value((int) future));
        assertThat(jdbc.queryForObject("SELECT status FROM care_task WHERE id=?", String.class, future))
                .isEqualTo("CANCELLED");
        for (long kept : new long[] {earlier, other})
            assertThat(jdbc.queryForObject("SELECT status FROM care_task WHERE id=?", String.class, kept))
                    .isEqualTo("NOT_YET_DUE");
        assertThat(jdbc.queryForObject("SELECT status FROM care_task WHERE id=?", String.class, due))
                .isEqualTo("DUE");
    }

    private CareDailyWork daily(String name) {
        return context.getBeansOfType(CareDailyWork.class).values().stream()
                .filter(w -> w.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Chưa cài việc " + name));
    }

    @Test
    void FR_012_dayStartDeterminesFromTheExistingScheduleWithoutAttendanceRecords() {
        var work = daily("DAY_CARE_DAY_START");
        work.run(date, at);
        work.run(date, at);
        assertThat(api().onDate(resident, date).orElseThrow().status()).isEqualTo(AttendanceStatus.NOT_ARRIVED);
        assertThat(events.stream(AttendanceDetermined.class)
                        .filter(e -> e.residentId() == resident)
                        .count())
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M04.ATTENDANCE_RECORD' AND entity_id=?",
                        Integer.class,
                        resident))
                .isZero();
    }

    @Test
    void FR_013_noShowUsesCfgAtTheBoundaryAndNotifiesOnlyOnce() {
        api().refreshCalendar(resident, date);
        var work = context.getBeansOfType(CareMinuteWork.class).values().stream()
                .filter(w -> w.getClass().getName().contains("AttendanceClockWork"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Chưa cài bước vắng không báo"));
        var limit = date.atTime(8, 0)
                .atZone(BusinessTime.ZONE)
                .toInstant()
                .plus(context.getBean(ParameterApi.class)
                        .read(ParameterCode.CFG_M02_07)
                        .asDuration());
        work.run(limit.minusNanos(1));
        assertThat(api().onDate(resident, date).orElseThrow().status()).isEqualTo(AttendanceStatus.NOT_ARRIVED);
        work.run(limit);
        work.run(limit);
        assertThat(api().onDate(resident, date).orElseThrow().status()).isEqualTo(AttendanceStatus.UNREPORTED_ABSENT);
        assertThat(events.stream(UnreportedAbsence.class)
                        .filter(e -> e.residentId() == resident)
                        .count())
                .isEqualTo(1);
    }

    @Test
    void FR_019_dayEndNotifiesOnceWithoutAutomaticallyCheckingOut() throws Exception {
        arriveId();
        var work = daily("DAY_CARE_DAY_END");
        var tomorrow = date.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant();
        work.run(date, tomorrow);
        work.run(date, tomorrow);
        assertThat(api().onDate(resident, date).orElseThrow().status()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(events.stream(DayCarePresentAtDayEnd.class)
                        .filter(e -> e.residentId() == resident)
                        .count())
                .isEqualTo(1);
    }

    @Test
    void FR_015_timeCorrectionUpdatesTheProjectionButPreservesTheConfirmedOriginal() throws Exception {
        arriveId();
        long record = jdbc.queryForObject(
                "SELECT id FROM record_event WHERE kind='M04.ATTENDANCE_RECORD' AND entity_id=?", Long.class, resident);
        Instant corrected = date.atTime(9, 0).atZone(BusinessTime.ZONE).toInstant();
        mvc.perform(post("/api/v1/day-care-attendance-records/" + record + "/correct")
                        .header("Idempotency-Key", "correct-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"at\":\"" + corrected
                                + "\"},\"reason\":\"Sửa giờ điểm danh nhập nhầm\"}"))
                .andExpect(status().isCreated());
        assertThat(api().onDate(resident, date).orElseThrow().arrivedAt()).contains(corrected);
        assertThat(json.readTree(jdbc.queryForObject(
                                "SELECT content FROM record_event WHERE id=?", String.class, record))
                        .get("at")
                        .asText())
                .isEqualTo(at.toString());
    }

    @Test
    void FR_049_scopeProviderReadsAttendanceFromTheSharedRecord() throws Exception {
        long id = arriveId();
        var provider = context
                .getBeansOfType(com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider.class)
                .values()
                .stream()
                .filter(p -> p.entityType().equals("M04.DAY_CARE_ATTENDANCE"))
                .findFirst()
                .orElseThrow();
        var scope = provider.assignScope(id);
        assertThat(scope.residentId()).isEqualTo(resident);
        assertThat(scope.floorAreaId()).isEqualTo(floor);
    }

    @Test
    void FR_018a_returnTimeAuditContainsTheChangedTime() throws Exception {
        long id = arriveId();
        api().changeReturnTime(resident, date, LocalTime.of(18, 0), TestUsers.nextId());
        String after = jdbc.queryForObject(
                "SELECT data_after FROM audit_log WHERE entity_type='M04.DAY_CARE_ATTENDANCE' AND entity_id=? AND action='DAY_CARE_RETURN_TIME_CHANGED'",
                String.class,
                Long.toString(id));
        assertThat(json.readTree(after).get("currentExpectedReturn")).isNotNull();
        assertThat(json.readTree(after).get("currentExpectedReturn").asText()).startsWith("18:00");
    }
}
