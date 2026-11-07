package com.eldercare.backend.reporting;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.TestScopedMetrics.Event;
import com.eldercare.backend.reporting.TestScopedMetrics.EventSource;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SC-011, FR-012 (016 T011, {@code FR_012_PhamViTheoThoiGianIT}; quickstart Q10, Q11): phạm vi báo cáo theo thời gian chạy
 * trên ca, thành viên, phân công thật của 008: Điều dưỡng có hai ca, bổ sung, gỡ, vắng, nghỉ có duyệt, ca hủy; Người phụ
 * trách ca; Trưởng tầng giao tạm, đã thôi giao. Biên CFG-M15-07 mặc định 2 giờ.
 */
@AutoConfigureMockMvc
@Import(TestScopedMetrics.class)
class FR_012_TimeScopeIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    EventSource testHealth;

    ReportScopeFixture fixture;
    long floor;
    long resident;
    long otherResident;
    Instant day1;
    Instant day2;

    @BeforeEach
    void setUp() {
        fixture = new ReportScopeFixture(jdbc, time);
        floor = TestUsers.nextId();
        resident = TestUsers.nextId();
        otherResident = TestUsers.nextId();
        LocalDate today = time.today();
        day1 = time.startOfDay(today.minusDays(5)).plus(Duration.ofHours(6));
        day2 = day1.plus(Duration.ofDays(1));
    }

    @AfterEach
    void reset() {
        testHealth.events.clear();
    }

    private static Instant plusHours(Instant at, int hours) {
        return at.plus(Duration.ofHours(hours));
    }

    private void event(Instant at, Long floorArea, Long residentId) {
        testHealth.events.add(new Event("e-" + TestUsers.nextId(), at, floorArea, residentId));
    }

    private void expectCount(TestUser user, int count) throws Exception {
        LocalDate today = time.today();
        mvc.perform(get("/api/v1/reports/HEALTH_MEDICATION")
                        .param("from", today.minusDays(7).toString())
                        .param("to", today.minusDays(1).toString())
                        .header(
                                "Authorization",
                                TestUsers.bearer(fixture.users().session(user.accountId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.values[?(@.metricCode == '" + TestScopedMetrics.HEALTH + "')].value")
                        .value(contains(count)));
    }

    /** Điều dưỡng có tên trong ca của tầng, phân công người cao tuổi {@code resident}. */
    private long nurseShift(TestUser nurse, Instant start) {
        long shift = fixture.shift(floor, start, "CLOSED");
        fixture.member(shift, nurse.staffId());
        fixture.assignResident(shift, nurse.staffId(), resident, start, plusHours(start, 8));
        return shift;
    }

    @Test
    void Q11_nurse_twoShifts_assignedResidentAndCommonWork_withinShiftHoursPlusMargin() throws Exception {
        TestUser nurse = fixture.staff(Role.NURSING_STAFF);
        nurseShift(nurse, day1);
        nurseShift(nurse, day2);
        event(plusHours(day1, 1), floor, resident);
        event(plusHours(day2, 1), floor, resident);
        event(plusHours(day1, 2), floor, null);
        event(plusHours(day1, 9), floor, resident);
        event(plusHours(day1, 11), floor, resident);
        event(plusHours(day1, 1), floor, otherResident);
        event(plusHours(day1, 1), TestUsers.nextId(), null);

        expectCount(nurse, 4);
    }

    @Test
    void absentMidShift_countedUpToAbsenceOnly_noMarginAfter() throws Exception {
        TestUser nurse = fixture.staff(Role.NURSING_STAFF);
        long shift = fixture.shift(floor, day1, "CLOSED");
        Instant absentAt = plusHours(day1, 3);
        fixture.member(shift, nurse.staffId());
        fixture.assignResidentEnded(shift, nurse.staffId(), resident, day1, plusHours(day1, 8), absentAt);
        jdbc.update("UPDATE shift_staff SET absent_at=? WHERE shift_id=?", ShiftStore.sql(absentAt), shift);
        event(plusHours(day1, 2), floor, resident);
        event(plusHours(day1, 4), floor, resident);

        expectCount(nurse, 1);
    }
}
