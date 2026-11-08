package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.care.api.*;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.internal.attendance.service.AttendanceClockWork;
import com.eldercare.backend.care.internal.scheduling.service.*;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.fake.admission.*;
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
/**
 * BR-M04-02, FR-013, FR-014 (T027, T029, T030): nguồn thật của 004 ({@code DayCareCalendarApi}, lệnh báo vắng và ngày nghỉ)
 * và 012 ({@code DayCarePickupPort} của {@code PickupPorts}); không dùng bản giả của care cho lịch và bản ghi đón.
 */
@Import({
    SwitchableUserConfig.class,
    FakeM01Config.class,
    FakeM02Config.class,
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
    TestClockControl clock;

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
        // Giữa ngày: sau giờ đến 08:00 quá CFG-M02-07, trước giờ về 17:00; không phụ thuộc giờ chạy test
        clock.setTo(date.atTime(12, 0).atZone(BusinessTime.ZONE).toInstant());
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
        locations.floor(floor, null).daycareRestLocation(new LocationRef("FLOOR", floor));
    }

    @AfterEach
    void resetClock() {
        clock.reset();
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
    void BR_M04_02_onlyValidPickupCanCheckOutAndItsRecordIsMarkedUsed() throws Exception {
        long id = arriveId(), pickup = TestUsers.nextId();
        leave(id, pickup, at.plusSeconds(60)).andExpect(status().isConflict());
        selfReturnPickup(pickup, at);
        leave(id, pickup, at.plusSeconds(60))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendance.checkoutType").value("SELF_RETURN"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M10.PICKUP_RECORD_EVENT' AND entity_type='M10.PICKUP_RECORD' AND entity_id=?",
                        Integer.class,
                        pickup))
                .isEqualTo(1);
        assertThat(api().onDate(resident, date).orElseThrow().status()).isEqualTo(AttendanceStatus.LEFT);
    }

    /** Bản ghi đón Tự về Chưa dùng của 012 (bảng {@code pickup_record}), còn hiệu lực một giờ. */
    private void selfReturnPickup(long id, Instant recordedAt) {
        jdbc.update(
                "INSERT INTO pickup_record(id,resident_id,type,basis,at,handover_staff_id,recorded_by,expires_at,created_at) VALUES(?,?,'SELF_RETURN','SELF_RETURN',?,?,?,?,?)",
                id,
                resident,
                LocalDateTime.ofInstant(recordedAt, ZoneOffset.UTC),
                user.accountId(),
                user.accountId(),
                LocalDateTime.ofInstant(recordedAt.plusSeconds(3600), ZoneOffset.UTC),
                LocalDateTime.ofInstant(recordedAt, ZoneOffset.UTC));
    }

    private org.springframework.test.web.servlet.ResultActions notice(Instant familyNotifiedAt) throws Exception {
        return mvc.perform(post("/api/v1/day-care-absence-notices")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"residentId\":" + resident + ",\"date\":\"" + date + "\",\"familyNotifiedAt\":\""
                        + familyNotifiedAt + "\"}"));
    }

    private Instant timely() {
        return date.atTime(8, 0).atZone(BusinessTime.ZONE).toInstant().minus(Duration.ofDays(2));
    }

    private AttendanceStatus attendance() {
        return api().onDate(resident, date).orElseThrow().status();
    }

    private Long stored(String field) {
        String value = jdbc.queryForObject(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(content, ?)) FROM simple_record WHERE unique_key=?",
                String.class,
                "$." + field,
                "M04.DAY_CARE_ATTENDANCE:" + resident + ":" + date);
        return value == null || value.equals("null") ? null : Long.valueOf(value);
    }

    private List<Long> notices() {
        return jdbc.queryForList(
                "SELECT id FROM absence WHERE kind='DAY_CARE_NOTICE' AND resident_id=? ORDER BY id",
                Long.class,
                resident);
    }

    @Test
    void FR_013_dayStartReadsCalendarOf004_withoutNoticeNotArrived() {
        daily("DAY_CARE_DAY_START").run(date, time.now());
        assertThat(attendance()).isEqualTo(AttendanceStatus.NOT_ARRIVED);
    }

    @Test
    void FR_013_timelyNoticeRecordedBy004_makesReportedAbsentAndBlocksArrival() throws Exception {
        notice(timely())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.withinNoticePeriod").value(true));
        assertThat(attendance()).isEqualTo(AttendanceStatus.REPORTED_ABSENT);
        assertThat(stored("timelyAbsenceId")).isEqualTo(notices().getFirst());
        arrive().andExpect(status().isConflict());
    }

    @Test
    void FR_014_lateNoticeKeepsStateAndIsStored_laterTimelyNoticeStillCounts() throws Exception {
        daily("DAY_CARE_DAY_START").run(date, time.now());
        notice(time.now())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.withinNoticePeriod").value(false));
        assertThat(attendance()).isEqualTo(AttendanceStatus.NOT_ARRIVED);
        assertThat(stored("lateAbsenceId")).isEqualTo(notices().getFirst());
        assertThat(stored("timelyAbsenceId")).isNull();
        notice(timely()).andExpect(status().isCreated());
        assertThat(attendance()).isEqualTo(AttendanceStatus.REPORTED_ABSENT);
        assertThat(stored("timelyAbsenceId")).isEqualTo(notices().get(1));
        assertThat(stored("lateAbsenceId")).isEqualTo(notices().getFirst());
    }

    @Test
    void FR_013_closureDayDeclaredBy004_cancelsTodaysAttendance() throws Exception {
        daily("DAY_CARE_DAY_START").run(date, time.now());
        mvc.perform(post("/api/v1/day-care-closure-days")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"" + date + "\",\"reason\":\"Nghỉ lễ\"}"))
                .andExpect(status().isCreated());
        assertThat(attendance()).isEqualTo(AttendanceStatus.HOLIDAY_CANCELLED);
        context.getBean(AttendanceClockWork.class).run(time.now());
        assertThat(attendance()).isEqualTo(AttendanceStatus.HOLIDAY_CANCELLED);
    }

    @Test
    void CFG_M02_07_noShowPastLimitBecomesUnreportedAbsent_withCalendarOf004() {
        daily("DAY_CARE_DAY_START").run(date, time.now());
        context.getBean(AttendanceClockWork.class).run(time.now());
        assertThat(attendance()).isEqualTo(AttendanceStatus.UNREPORTED_ABSENT);
        assertThat(events.stream(UnreportedAbsence.class)
                        .filter(e -> e.residentId() == resident)
                        .count())
                .isEqualTo(1);
    }

    @Test
    void FR_019_stillPresentAtDayEndNotifiesOnce() throws Exception {
        arriveId();
        Instant nextMorning =
                date.plusDays(1).atTime(0, 5).atZone(BusinessTime.ZONE).toInstant();
        assertThat(daily("DAY_CARE_DAY_END").run(date, nextMorning)).isEqualTo(1);
        assertThat(daily("DAY_CARE_DAY_END").run(date, nextMorning)).isZero();
        assertThat(events.stream(DayCarePresentAtDayEnd.class)
                        .filter(e -> e.residentId() == resident)
                        .count())
                .isEqualTo(1);
        assertThat(attendance()).isEqualTo(AttendanceStatus.PRESENT);
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

    private CareDailyWork daily(String name) {
        return context.getBeansOfType(CareDailyWork.class).values().stream()
                .filter(w -> w.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Chưa cài việc " + name));
    }
}
