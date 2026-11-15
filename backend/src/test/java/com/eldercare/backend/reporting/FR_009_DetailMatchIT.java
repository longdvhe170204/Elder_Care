package com.eldercare.backend.reporting;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.reporting.TestScopedMetrics.Event;
import com.eldercare.backend.reporting.TestScopedMetrics.EventSource;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
 * FR-009, SC-006, US7 #1, #2 (016 T021, {@code FR_009_ChiTietKhopSoIT}; quickstart Q12, Q13): dashboard, báo cáo cùng ngày
 * và danh sách chi tiết của một chỉ tiêu cho cùng số, cùng phạm vi; danh sách phân trang đủ dòng; người cao tuổi ngoài
 * phạm vi hiện hành ở dạng rút gọn, không mở được bản ghi. Chỉ tiêu giả tới khi các module nguồn có {@code *ThongKeApi};
 * mở bản ghi chỉ xem ở feature sở hữu (Q12) kiểm cùng chỉ tiêu thật.
 */
@AutoConfigureMockMvc
@Import(TestScopedMetrics.class)
class FR_009_DetailMatchIT extends MySqlIntegrationTestBase {

    private static final int PER_FLOOR = 3;

    // mỗi test một mốc cách nhau 1 phút để bộ đệm 20 giây của dashboard không còn hiệu lực
    private static int step;

    /** Chỉ tiêu test có ở cả dashboard và báo cáo: mã → (khối dashboard, nhóm báo cáo). */
    private static final Map<String, List<String>> METRICS = Map.of(
            TestScopedMetrics.RESIDENTS, List.of("RESIDENTS", "RESIDENTS"),
            TestScopedMetrics.TASKS, List.of("CARE_TASKS", "CARE"),
            TestScopedMetrics.HEALTH, List.of("HEALTH_EVENTS", "HEALTH_MEDICATION"));

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    EventSource testResidents;

    @Autowired
    EventSource testTasks;

    @Autowired
    EventSource testHealth;

    ReportScopeFixture fixture;
    TestUser manager;
    TestUser floorManager;
    long floor;
    long otherFloor;

    private List<EventSource> sources() {
        return List.of(testResidents, testTasks, testHealth);
    }

    @BeforeEach
    void setUp() {
        clock.setTo(time.startOfDay(time.today()).plus(Duration.ofHours(10)).plus(Duration.ofMinutes(++step)));
        Instant now = time.now();
        fixture = new ReportScopeFixture(jdbc, time);
        manager = fixture.staff(Role.NURSING_HOME_MANAGER);
        floorManager = fixture.staff(Role.FLOOR_MANAGER);
        floor = TestUsers.nextId();
        otherFloor = TestUsers.nextId();
        fixture.floorManager(floorManager.staffId(), floor, now.minus(Duration.ofDays(30)), null);
        for (EventSource source : sources()) {
            for (int i = 0; i < PER_FLOOR; i++) {
                source.events.add(event(now, floor));
                source.events.add(event(now, otherFloor));
            }
        }
    }

    private static Event event(Instant now, long floorArea) {
        return new Event("e-" + TestUsers.nextId(), now.minus(Duration.ofHours(1)), floorArea, TestUsers.nextId());
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

    private MockHttpServletRequestBuilder today(String url) {
        LocalDate today = time.today();
        return get(url).param("from", today.toString()).param("to", today.toString());
    }

    private void expectSameNumberEverywhere(TestUser user, int expected) throws Exception {
        for (var e : METRICS.entrySet()) {
            String metric = e.getKey();
            String block = e.getValue().get(0);
            String report = e.getValue().get(1);
            perform(user, get("/api/v1/dashboard"))
                    .andExpect(jsonPath("$.blocks[?(@.group == '" + block + "')].values[?(@.metricCode == '" + metric
                                    + "')].value")
                            .value(contains(expected)));
            perform(user, today("/api/v1/reports/" + report))
                    .andExpect(jsonPath("$.values[?(@.metricCode == '" + metric + "')].value")
                            .value(contains(expected)));
            perform(user, today("/api/v1/metric-details/" + metric))
                    .andExpect(jsonPath("$.total").value(expected))
                    .andExpect(jsonPath("$.rows", hasSize(expected)));
            perform(user, get("/api/v1/metric-details/" + metric))
                    .andExpect(jsonPath("$.total").value(expected));
        }
    }

    @Test
    void SC_006_dashboardReportAndDetails_sameNumber_facilityWide() throws Exception {
        expectSameNumberEverywhere(manager, 2 * PER_FLOOR);
    }

    @Autowired
    List<MetricSource<?>> metricSources;

    private static String body(ResultActions result) throws Exception {
        return result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static int total(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(body(result), "$.total")).intValue();
    }
}
