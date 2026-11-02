package com.eldercare.backend.reporting;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * FR-042a, US2 #1, #9 (016 T011, {@code FR_042a_SuCoHanhChinhIT}; quickstart Q6): Hành chính chỉ thấy nhóm sự cố "không
 * thuộc sức khỏe", ở báo cáo và dashboard, không có kết quả (mô tả, diễn biến), không có gì về sự cố khác. Quản lý viện
 * thấy sự cố ở nhóm sức khỏe, không có khối riêng. Dùng chỉ tiêu giả tới khi 007 có phần đọc sự cố; Q7 (đổi loại sự cố)
 * chờ 007.
 */
@AutoConfigureMockMvc
@Import(TestScopedMetrics.class)
class FR_042a_AdminIncidentIT extends MySqlIntegrationTestBase {

    private static final String REPORT = "/api/v1/reports/HEALTH_MEDICATION";
    private static final String NON_HEALTH_BLOCK = "$.blocks[?(@.group == 'NON_HEALTH_INCIDENTS')]";
    private static final String HEALTH_BLOCK = "$.blocks[?(@.group == 'HEALTH_EVENTS')]";

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
    EventSource testNonHealthIncidents;

    @Autowired
    EventSource testHealth;

    TestUsers users;
    TestUser office;

    @BeforeEach
    void setUp() {
        clock.setTo(time.startOfDay(time.today()).plus(Duration.ofHours(10)).plus(Duration.ofMinutes(++step)));
        users = new TestUsers(jdbc, time);
        office = users.staff(Role.OFFICE_STAFF);
        Instant at = time.now().minus(Duration.ofHours(1));
        long floor = TestUsers.nextId();
        for (int i = 0; i < 3; i++) {
            testNonHealthIncidents.events.add(new Event("lost-" + TestUsers.nextId(), at, floor, TestUsers.nextId()));
        }
        for (int i = 0; i < 2; i++) {
            testHealth.events.add(new Event("fall-" + TestUsers.nextId(), at, floor, TestUsers.nextId()));
        }
    }

    @AfterEach
    void reset() {
        testNonHealthIncidents.events.clear();
        testHealth.events.clear();
        clock.reset();
    }

    private ResultActions perform(TestUser user, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", TestUsers.bearer(users.session(user.accountId()))));
    }

    private MockHttpServletRequestBuilder ranged(String url) {
        LocalDate today = time.today();
        return get(url).param("from", today.minusDays(7).toString()).param("to", today.toString());
    }

    @Test
    void office_report_onlyNonHealthIncidents() throws Exception {
        perform(office, ranged(REPORT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.values[?(@.metricCode == '" + TestScopedMetrics.NON_HEALTH + "')].value")
                        .value(contains(3)))
                .andExpect(jsonPath("$.values[?(@.metricCode == '" + TestScopedMetrics.HEALTH + "')]")
                        .value(empty()));
    }

    @Test
    void office_details_noDescription_otherIncidentsDenied() throws Exception {
        perform(office, ranged("/api/v1/metric-details/" + TestScopedMetrics.NON_HEALTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.rows", hasSize(3)))
                .andExpect(jsonPath("$.rows[0].result.item").doesNotExist());
        perform(office, ranged("/api/v1/metric-details/" + TestScopedMetrics.HEALTH))
                .andExpect(status().isForbidden());
    }

    @Test
    void US2_1_office_dashboard_ownBlockOnly() throws Exception {
        perform(office, get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NON_HEALTH_BLOCK + ".values[0].value").value(contains(3)))
                .andExpect(jsonPath(HEALTH_BLOCK).value(empty()));
    }

    @Test
    void manager_dashboard_incidentsInHealthBlock_noSeparateBlock() throws Exception {
        perform(users.staff(Role.NURSING_HOME_MANAGER), get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(HEALTH_BLOCK).value(not(empty())))
                .andExpect(jsonPath(NON_HEALTH_BLOCK).value(empty()));
    }
}
