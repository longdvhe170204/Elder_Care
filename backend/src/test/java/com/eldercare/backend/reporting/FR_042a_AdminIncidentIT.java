package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.incident.api.IncidentStatisticsApi;
import com.eldercare.backend.incident.api.dto.AlertReportView;
import com.eldercare.backend.incident.api.dto.IncidentReportView;
import com.eldercare.backend.incident.api.dto.MonitoringView;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * FR-042a, US2 #1, #9 (016 T011, {@code FR_042a_SuCoHanhChinhIT}; quickstart Q6, Q7): Hành chính chỉ thấy chỉ tiêu
 * {@code HEALTH.NON_HEALTH_INCIDENTS} (sự cố thuộc loại mang dấu "không thuộc sức khỏe"), không có kết quả, không có gì
 * về sự cố khác; sự cố đổi sang loại không mang dấu rời khỏi số từ lần lấy kế tiếp. Quản lý viện thấy mọi sự cố ở
 * {@code HEALTH.INCIDENTS}. Chỉ tiêu thật trên nguồn 007 giả (dấu theo danh mục hiện hành là phần của 007).
 */
@AutoConfigureMockMvc
@Import(FR_042a_AdminIncidentIT.Source.class)
class FR_042a_AdminIncidentIT extends MySqlIntegrationTestBase {

    private static final String REPORT = "/api/v1/reports/HEALTH_MEDICATION";
    private static final String NON_HEALTH = "HEALTH.NON_HEALTH_INCIDENTS";
    private static final String INCIDENTS = "HEALTH.INCIDENTS";

    /** Nguồn sự cố của 007 thay bằng danh sách sửa được trong test; mọi người xem ở đây toàn viện. */
    static class Incidents implements IncidentStatisticsApi {

        final List<IncidentReportView> rows = new CopyOnWriteArrayList<>();

        @Override
        public Stream<AlertReportView> alerts(ScopeFilter scope, Instant from, Instant to) {
            return Stream.empty();
        }

        @Override
        public Stream<IncidentReportView> incidents(ScopeFilter scope, Instant from, Instant to) {
            return rows.stream()
                    .filter(r -> !r.createdAt().isBefore(from) && r.createdAt().isBefore(to));
        }

        @Override
        public List<MonitoringView> underMonitoring(ScopeFilter scope) {
            return List.of();
        }

        @Override
        public List<Long> criticalResidentIds(ScopeFilter scope) {
            return List.of();
        }

        @Override
        public int cancelledCount(ScopeFilter scope, Instant from, Instant to) {
            return 0;
        }

        @Override
        public List<com.eldercare.backend.incident.api.dto.IsolationZoneView> containmentZones(ScopeFilter scope) {
            return List.of();
        }
    }

    @TestConfiguration
    static class Source {
        @Bean
        @Primary
        Incidents testIncidents() {
            return new Incidents();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    Incidents incidents;

    // mỗi test một mốc cách nhau 1 phút để bộ đệm 20 giây của dashboard không còn hiệu lực
    private static int step;

    TestUsers users;
    TestUser office;
    Instant at;
    long floor;

    @BeforeEach
    void setUp() {
        clock.setTo(time.startOfDay(time.today()).plus(Duration.ofHours(10)).plus(Duration.ofMinutes(++step)));
        users = new TestUsers(jdbc, time);
        office = users.staff(Role.OFFICE_STAFF);
        at = time.now().minus(Duration.ofHours(1));
        floor = TestUsers.nextId();
        incidents.rows.add(incident(1, "LOST_BELONGING", true));
        incidents.rows.add(incident(2, "LOST_BELONGING", true));
        incidents.rows.add(incident(3, "FACILITY", true));
        incidents.rows.add(incident(4, "FALL", false));
        incidents.rows.add(incident(5, "FALL", false));
    }

    @AfterEach
    void reset() {
        incidents.rows.clear();
        clock.reset();
    }

    private IncidentReportView incident(long id, String type, boolean nonHealth) {
        return new IncidentReportView(
                id, TestUsers.nextId(), "MINOR", null, type, nonHealth, floor, at, null, 0, "NEW");
    }

    private ResultActions perform(TestUser user, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", TestUsers.bearer(users.session(user.accountId()))));
    }

    private MockHttpServletRequestBuilder ranged(String url) {
        LocalDate today = time.today();
        return get(url).param("from", today.minusDays(7).toString())
                .param("to", today.plusDays(1).toString());
    }

    private static String value(String code) {
        return "$.values[?(@.metricCode == '" + code + "')].value";
    }

    @Test
    void office_report_onlyNonHealthIncidents_detailWithoutResult() throws Exception {
        perform(office, ranged(REPORT))
                .andExpect(status().isOk())
                .andExpect(jsonPath(value(NON_HEALTH)).value(contains(3)))
                .andExpect(jsonPath("$.values[?(@.metricCode == '" + INCIDENTS + "')]")
                        .value(empty()));

        String body = perform(office, ranged("/api/v1/metric-details/" + NON_HEALTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(body).doesNotContain("FALL").doesNotContain("escalationCount");

        // không có nhóm sức khỏe: 403 hoặc danh sách rỗng, không lộ sự cố khác
        assertThat(perform(office, ranged("/api/v1/metric-details/" + INCIDENTS))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .doesNotContain("FALL");
    }

    @Test
    void Q7_typeChangedToHealthType_leavesOfficeNumbersOnNextRead() throws Exception {
        perform(office, ranged(REPORT)).andExpect(jsonPath(value(NON_HEALTH)).value(contains(3)));

        incidents.rows.replaceAll(r -> r.incidentId() == 3 ? incident(3, "FALL", false) : r);

        perform(office, ranged(REPORT)).andExpect(jsonPath(value(NON_HEALTH)).value(contains(2)));
    }

    @Test
    void manager_seesAllIncidentsInHealthGroup() throws Exception {
        perform(users.staff(Role.NURSING_HOME_MANAGER), ranged(REPORT))
                .andExpect(status().isOk())
                .andExpect(jsonPath(value(INCIDENTS)).value(contains(5)));
    }
}
