package com.eldercare.backend.reporting;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.TestDoseMetric.Dose;
import com.eldercare.backend.reporting.TestDoseMetric.DoseSource;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
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

    /**
     * 016 T109 (FR-007): khoảng báo cáo giao với dữ liệu đã loại bỏ theo bản tóm tắt EXPIRED_DATA_PURGED thì chỉ tiêu đọc
     * loại dữ liệu đó mang nhãn và tên loại. Mốc năm 2001 để không ảnh hưởng báo cáo của test khác (audit_log giữ lại).
     */
    @Test
    void FR_007_purgedRangeOverlapsPeriod_labelsMetricWithDataType() throws Exception {
        jdbc.update(
                "INSERT INTO audit_log (occurred_at, actor_type, action, entity_type, entity_id, details, exempt_from_purge)"
                        + " VALUES (?, 'SCHEDULER', 'EXPIRED_DATA_PURGED', 'M15.PURGE_RUN', ?, ?, 1)",
                java.time.LocalDateTime.ofInstant(time.now(), BusinessTime.ZONE),
                "run-" + UUID.randomUUID(),
                "{\"deleted\":{\"M07.TEST_DOSE\":{\"count\":2,\"from\":\"2001-01-01T00:00:00Z\",\"to\":\"2001-03-01T00:00:00Z\"}}}");
        perform(manager, get(REPORT).param("from", "2001-02-01").param("to", "2001-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(VALUE + ".labels[*]").value(hasItem("PURGED_BY_RETENTION")))
                .andExpect(jsonPath(VALUE + ".purgedDataTypes[*]").value(hasItem("M07.TEST_DOSE")));
        perform(manager, get(REPORT).param("from", "2001-04-01").param("to", "2001-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(VALUE + ".purgedDataTypes[*]").isEmpty());
    }
}
