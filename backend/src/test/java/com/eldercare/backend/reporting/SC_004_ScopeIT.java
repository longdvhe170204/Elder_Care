package com.eldercare.backend.reporting;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.reporting.TestScopedMetrics.Event;
import com.eldercare.backend.reporting.TestScopedMetrics.EventSource;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * SC-004, US2 (016 T011, {@code SC_004_PhamViIT}; quickstart Q3 → Q9): bộ 11 tài khoản kiểm thử quyền trên phân công thật
 * của 008, mở cùng các báo cáo và dashboard. Mỗi nhóm chỉ tiêu có cùng sáu bản ghi: tầng 1 có hai người và một việc chung,
 * tầng 2, 3, 4 mỗi tầng một người; số đúng theo bảng FR-010, FR-012, FR-015; ô "—" bị từ chối.
 */
@AutoConfigureMockMvc
@Import({TestScopedMetrics.class, FakeLocations.Config.class})
class SC_004_ScopeIT extends MySqlIntegrationTestBase {

    private static final int ALL = 6;
    private static final int FLOOR_1 = 3;
    private static final int NURSE_ON_FLOOR_1 = 2;

    // mỗi test một mốc cách nhau 1 phút để bộ đệm 20 giây của dashboard không còn hiệu lực
    private static int step;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeLocations locations;

    @Autowired
    EventSource testResidents;

    @Autowired
    EventSource testTasks;

    @Autowired
    EventSource testHealth;

    @Autowired
    EventSource testCosts;

    @Autowired
    EventSource testStaffing;

    ReportScopeFixture fixture;
    long floor1;
    long floor2;
    TestUser manager;
    TestUser floorManager1;
    TestUser temporaryFloorManager3;
    TestUser formerFloorManager4;
    TestUser doctor;
    TestUser nurseFloor1;
    TestUser shiftLeadFloor1;
    TestUser office;
    TestUser caregiver;
    TestUser officeAndNurseFloor2;
    TestUser doctorAndFloorManager2;

    private List<EventSource> sources() {
        return List.of(testResidents, testTasks, testHealth, testCosts, testStaffing);
    }

    @BeforeEach
    void setUp() {
        clock.setTo(time.startOfDay(time.today()).plus(Duration.ofHours(10)).plus(Duration.ofMinutes(++step)));
        Instant now = time.now();
        fixture = new ReportScopeFixture(jdbc, time);
        floor1 = TestUsers.nextId();
        floor2 = TestUsers.nextId();
        long floor3 = TestUsers.nextId();
        long floor4 = TestUsers.nextId();
        locations.floor(floor1, TestUsers.nextId());
        locations.floor(floor2, TestUsers.nextId());

        manager = fixture.staff(Role.NURSING_HOME_MANAGER);
        floorManager1 = fixture.staff(Role.FLOOR_MANAGER);
        temporaryFloorManager3 = fixture.staff(Role.FLOOR_MANAGER);
        formerFloorManager4 = fixture.staff(Role.FLOOR_MANAGER);
        doctor = fixture.staff(Role.DOCTOR);
        nurseFloor1 = fixture.staff(Role.NURSING_STAFF);
        shiftLeadFloor1 = fixture.staff(Role.NURSING_STAFF);
        office = fixture.staff(Role.OFFICE_STAFF);
        caregiver = fixture.staff(Role.CAREGIVER);
        officeAndNurseFloor2 = fixture.staff(Role.OFFICE_STAFF, Role.NURSING_STAFF);
        doctorAndFloorManager2 = fixture.staff(Role.DOCTOR, Role.FLOOR_MANAGER);

        Instant longAgo = now.minus(Duration.ofDays(60));
        fixture.floorManager(floorManager1.staffId(), floor1, longAgo, null);
        fixture.floorManager(
                temporaryFloorManager3.staffId(), floor3, now.minusSeconds(60), now.plus(Duration.ofDays(3)));
        fixture.floorManager(formerFloorManager4.staffId(), floor4, longAgo, now.minus(Duration.ofDays(30)));
        fixture.floorManager(doctorAndFloorManager2.staffId(), floor2, longAgo, null);

        long resident1 = TestUsers.nextId();
        long resident2 = TestUsers.nextId();
        Instant start = now.minus(Duration.ofHours(2));
        Instant end = start.plus(Duration.ofHours(8));
        long shift1 = fixture.shift(floor1, start, "PUBLISHED");
        fixture.member(shift1, nurseFloor1.staffId());
        fixture.assignResident(shift1, nurseFloor1.staffId(), resident1, start, end);
        fixture.member(shift1, shiftLeadFloor1.staffId());
        fixture.lead(shift1, shiftLeadFloor1.staffId(), start);
        long shift2 = fixture.shift(floor2, start, "PUBLISHED");
        fixture.member(shift2, officeAndNurseFloor2.staffId());
        fixture.assignResident(shift2, officeAndNurseFloor2.staffId(), resident2, start, end);

        Instant at = now.minus(Duration.ofHours(1));
        for (EventSource source : sources()) {
            source.events.addAll(List.of(
                    event(at, floor1, resident1),
                    event(at, floor1, TestUsers.nextId()),
                    event(at, floor1, null),
                    event(at, floor2, resident2),
                    event(at, floor3, TestUsers.nextId()),
                    event(at, floor4, TestUsers.nextId())));
        }
    }

    private static Event event(Instant at, Long floor, Long resident) {
        return new Event("e-" + TestUsers.nextId(), at, floor, resident);
    }

    @AfterEach
    void reset() {
        sources().forEach(s -> s.events.clear());
        clock.reset();
    }

    private ResultActions perform(TestUser user, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(
                request.header("Authorization", TestUsers.bearer(fixture.users().session(user.accountId()))));
    }

    private MockHttpServletRequestBuilder report(String group) {
        LocalDate today = time.today();
        return get("/api/v1/reports/" + group)
                .param("from", today.minusDays(7).toString())
                .param("to", today.toString());
    }

    private void expectReport(TestUser user, String group, String metric, int count) throws Exception {
        perform(user, report(group))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.values[?(@.metricCode == '" + metric + "')].value")
                        .value(contains(count)));
    }

    private void expectDenied(TestUser user, String group) throws Exception {
        perform(user, report(group)).andExpect(status().isForbidden());
    }

    private void expectBlock(TestUser user, String group, String metric, int count) throws Exception {
        perform(user, get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks[?(@.group == '" + group + "')].values[?(@.metricCode == '" + metric
                                + "')].value")
                        .value(contains(count)));
    }

    private void expectNoBlock(TestUser user, String group) throws Exception {
        perform(user, get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks[?(@.group == '" + group + "')]").value(empty()));
    }

    @Test
    void healthReport_byAccount() throws Exception {
        String group = "HEALTH_MEDICATION";
        String metric = TestScopedMetrics.HEALTH;
        expectReport(manager, group, metric, ALL);
        expectReport(floorManager1, group, metric, FLOOR_1);
        expectReport(temporaryFloorManager3, group, metric, 1);
        expectReport(formerFloorManager4, group, metric, 0);
        expectReport(doctor, group, metric, ALL);
        expectReport(nurseFloor1, group, metric, NURSE_ON_FLOOR_1);
        expectReport(shiftLeadFloor1, group, metric, FLOOR_1);
        expectDenied(caregiver, group);
        expectReport(officeAndNurseFloor2, group, metric, 1);
        expectReport(doctorAndFloorManager2, group, metric, ALL);
    }

    @Test
    void careReport_byAccount() throws Exception {
        String group = "CARE";
        String metric = TestScopedMetrics.TASKS;
        expectReport(manager, group, metric, ALL);
        expectReport(floorManager1, group, metric, FLOOR_1);
        expectReport(nurseFloor1, group, metric, NURSE_ON_FLOOR_1);
        expectDenied(office, group);
        expectDenied(caregiver, group);
        expectReport(officeAndNurseFloor2, group, metric, 1);
    }

    @Test
    void costReport_onlyManagerAndOffice() throws Exception {
        String group = "INCURRED_COSTS";
        String metric = TestScopedMetrics.COSTS;
        expectReport(manager, group, metric, ALL);
        expectReport(office, group, metric, ALL);
        expectReport(officeAndNurseFloor2, group, metric, ALL);
        expectDenied(floorManager1, group);
        expectDenied(doctor, group);
        expectDenied(nurseFloor1, group);
        expectDenied(caregiver, group);
        expectDenied(doctorAndFloorManager2, group);
    }

    @Test
    void staffingReport_managerAndFloorManagers() throws Exception {
        String group = "STAFFING_SHIFTS";
        String metric = TestScopedMetrics.STAFFING;
        expectReport(manager, group, metric, ALL);
        expectReport(floorManager1, group, metric, FLOOR_1);
        expectReport(doctorAndFloorManager2, group, metric, 1);
        expectDenied(doctor, group);
        expectDenied(nurseFloor1, group);
        expectDenied(shiftLeadFloor1, group);
        expectDenied(office, group);
        expectDenied(officeAndNurseFloor2, group);
    }

    @Test
    void FR_015_multipleRoles_eachGroupByOwnRole() throws Exception {
        expectReport(officeAndNurseFloor2, "RESIDENTS", TestScopedMetrics.RESIDENTS, ALL);
        expectBlock(officeAndNurseFloor2, "RESIDENTS", TestScopedMetrics.RESIDENTS, ALL);
        expectBlock(officeAndNurseFloor2, "HEALTH_EVENTS", TestScopedMetrics.HEALTH, 1);
    }

    @Test
    void dashboard_byAccount() throws Exception {
        String group = "HEALTH_EVENTS";
        String metric = TestScopedMetrics.HEALTH;
        expectBlock(manager, group, metric, ALL);
        expectBlock(floorManager1, group, metric, FLOOR_1);
        expectBlock(temporaryFloorManager3, group, metric, 1);
        expectBlock(doctor, group, metric, ALL);
        expectBlock(nurseFloor1, group, metric, NURSE_ON_FLOOR_1);
        expectBlock(shiftLeadFloor1, group, metric, FLOOR_1);
        expectNoBlock(office, group);
        expectBlock(office, "INCURRED_COSTS", TestScopedMetrics.COSTS, ALL);
        perform(caregiver, get("/api/v1/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    void Q4_noAssignment_noNumbers() throws Exception {
        perform(formerFloorManager4, get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks").value(empty()))
                .andExpect(jsonPath("$.message").value("Hiện không có phạm vi phân công"));
        perform(fixture.staff(Role.NURSING_STAFF), get("/api/v1/dashboard"))
                .andExpect(jsonPath("$.blocks").value(empty()));
    }

    @Test
    void Q9_filterOptions_onlyInScope_outOfScopeValueGivesNothing() throws Exception {
        perform(floorManager1, report("CARE"))
                .andExpect(jsonPath("$.filterOptions.facilityWide").value(false))
                .andExpect(jsonPath("$.filterOptions.floorAreas", hasSize(1)))
                .andExpect(jsonPath("$.filterOptions.floorAreas[0].id").value(floor1));
        perform(manager, report("CARE"))
                .andExpect(jsonPath("$.filterOptions.facilityWide").value(true))
                .andExpect(jsonPath("$.filterOptions.floorAreas", hasSize(0)));
        perform(floorManager1, report("CARE").param("floorAreaId", String.valueOf(floor2)))
                .andExpect(jsonPath("$.values[?(@.metricCode == '" + TestScopedMetrics.TASKS + "')].value")
                        .value(contains(0)));
    }
}
