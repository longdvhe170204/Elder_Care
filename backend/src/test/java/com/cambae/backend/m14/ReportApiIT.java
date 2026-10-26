package com.cambae.backend.m14;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m14.TestDoseMetric.Dose;
import com.cambae.backend.m14.TestDoseMetric.DoseSource;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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
 * FR-009, FR-012, FR-013, FR-060 (016 T014, T022 phần khung; research O-07): báo cáo một nhóm theo khoảng trả giá trị của
 * mọi chỉ tiêu người xem thấy, trong phạm vi; khoảng chứa hôm nay mang nhãn "ngày chưa kết thúc"; danh sách chi tiết của
 * một con số (hoặc một ô chia nhóm) có đúng số dòng của con số đó.
 */
@AutoConfigureMockMvc
@Import({FakeM15Config.class, TestDoseMetric.class})
class ReportApiIT extends MySqlIntegrationTestBase {

    private static final String REPORT = "/api/v1/reports/HEALTH_MEDICATION";
    private static final String DETAILS = "/api/v1/metric-details/" + TestDoseMetric.CODE;
    private static final String VALUE = "$.values[?(@.metricCode == '" + TestDoseMetric.CODE + "')]";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    DoseSource doses;

    TestUsers users;
    TestUser manager;
    TestUser floorManager;
    long floor1;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        floorManager = users.staff(Role.FLOOR_MANAGER);
        floor1 = TestUsers.nextId();
        long floor2 = TestUsers.nextId();
        assignments.assign(
                floorManager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor1,
                        null,
                        null,
                        null,
                        time.now().minus(Duration.ofDays(30)),
                        null));
        Instant yesterday = time.now().minus(Duration.ofDays(1));
        doses.doses.addAll(List.of(
                new Dose("d-" + UUID.randomUUID(), yesterday, floor1, 1L, "Nguyễn Văn A", "Sáng"),
                new Dose("d-" + UUID.randomUUID(), yesterday, floor1, 1L, "Nguyễn Văn A", "Tối"),
                new Dose("d-" + UUID.randomUUID(), yesterday, floor2, 3L, "Lê Văn C", "Sáng")));
    }

    @AfterEach
    void reset() {
        assignments.unassign(floorManager.staffId());
        doses.doses.clear();
    }

    private ResultActions perform(TestUser u, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", TestUsers.bearer(users.session(u.accountId()))));
    }

    private MockHttpServletRequestBuilder lastWeek(String url) {
        LocalDate today = time.today();
        return get(url).param("from", today.minusDays(7).toString()).param("to", today.toString());
    }

    @Test
    void FR_012_report_valuesWithinScope_dayNotEndedLabel() throws Exception {
        perform(manager, lastWeek(REPORT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group").value("HEALTH_MEDICATION"))
                .andExpect(jsonPath("$.asOf").exists())
                .andExpect(jsonPath(VALUE + ".value").value(contains(3)))
                .andExpect(jsonPath(VALUE + ".labels[*]").value(hasItem("DAY_NOT_ENDED")))
                .andExpect(jsonPath(VALUE + ".breakdown[*].key").value(contains("Sáng", "Tối")));
        perform(floorManager, lastWeek(REPORT))
                .andExpect(status().isOk())
                .andExpect(jsonPath(VALUE + ".value").value(contains(2)));
    }

    @Test
    void FR_013_filterNarrowsWithinScope() throws Exception {
        perform(manager, lastWeek(REPORT).param("floorAreaId", String.valueOf(floor1)))
                .andExpect(jsonPath(VALUE + ".value").value(contains(2)));
    }

    @Test
    void FR_060_rangeTooLong_422() throws Exception {
        LocalDate today = time.today();
        perform(
                        manager,
                        get(REPORT)
                                .param("from", today.minusYears(2).toString())
                                .param("to", today.toString()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("RANGE_EXCEEDS_MAXIMUM"));
    }

    @Test
    void FR_010_groupNotVisible_403() throws Exception {
        perform(users.staff(Role.ACCOUNTANT), lastWeek(REPORT)).andExpect(status().isForbidden());
    }

    @Test
    void FR_009_details_matchTheNumber_andBreakdownCell() throws Exception {
        perform(manager, lastWeek(DETAILS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricCode").value(TestDoseMetric.CODE))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.rows.length()").value(3));
        perform(manager, lastWeek(DETAILS).param("breakdownKey", "Tối"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].resident").value("Nguyễn Văn A"));
        perform(floorManager, lastWeek(DETAILS))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.rows[*].resident").value(not(hasItem("Lê Văn C"))));
    }

    @Test
    void details_unknownMetric_404() throws Exception {
        perform(manager, lastWeek("/api/v1/metric-details/KHONG.CO")).andExpect(status().isNotFound());
    }
}
