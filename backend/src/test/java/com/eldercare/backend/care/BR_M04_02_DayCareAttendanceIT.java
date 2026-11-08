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
import com.eldercare.backend.support.SwitchableUserConfig;
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
    SwitchableUserConfig.class,
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
