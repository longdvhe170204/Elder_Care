package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.TestScopedMetrics.Event;
import com.eldercare.backend.reporting.TestScopedMetrics.EventSource;
import com.eldercare.backend.reporting.internal.report.service.ReportService;
import com.eldercare.backend.reporting.internal.scope.service.ReportPeriods;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
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

/**
 * FR-014, SC-005, Q-133 (016 T011, {@code FR_014_GioiHanTruongIT}, {@code SC_005_TenThuocIT}; quickstart Q20): kết quả thuộc
 * nhóm thông tin bị giới hạn không có trong phản hồi chi tiết lẫn dòng đưa vào file xuất; chi phí loại Thuốc của Hành
 * chính, Kế toán chỉ còn mã vật phẩm. Dùng chỉ tiêu giả tới khi 010 có {@code ChiPhiThongKeApi}.
 */
@AutoConfigureMockMvc
@Import(TestScopedMetrics.class)
class FR_014_FieldRestrictionIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    ReportService reports;

    @Autowired
    ReportPeriods periods;

    @Autowired
    EventSource testCosts;

    TestUsers users;
    LocalDate from;
    LocalDate to;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        from = time.today().minusDays(7);
        to = time.today().minusDays(1);
        testCosts.events.add(new Event(
                "cost-" + TestUsers.nextId(),
                time.startOfDay(to).minus(Duration.ofDays(1)),
                TestUsers.nextId(),
                TestUsers.nextId()));
    }

    @AfterEach
    void reset() {
        testCosts.events.clear();
    }

    private ResultActions details(TestUser user) throws Exception {
        return mvc.perform(get("/api/v1/metric-details/" + TestScopedMetrics.COSTS)
                .param("from", from.toString())
                .param("to", to.toString())
                .header("Authorization", TestUsers.bearer(users.session(user.accountId()))));
    }

    private void expectItemCodeOnly(TestUser user) throws Exception {
        String body = details(user)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].result.itemCode").value(TestScopedMetrics.ITEM_CODE))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(body).doesNotContain(TestScopedMetrics.RESTRICTED);
    }

    @Test
    void SC_005_office_andAccountant_seeItemCodeOnly() throws Exception {
        expectItemCodeOnly(users.staff(Role.OFFICE_STAFF));
        expectItemCodeOnly(users.staff(Role.ACCOUNTANT));
    }
}
