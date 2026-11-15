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
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
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

/**
 * FR-014, SC-005, Q-133 (016 T011, {@code FR_014_GioiHanTruongIT}, {@code SC_005_TenThuocIT}; quickstart Q20): kết quả thuộc
 * nhóm thông tin bị giới hạn không có trong phản hồi chi tiết lẫn dòng đưa vào file xuất; chi phí loại Thuốc của Hành
 * chính, Kế toán chỉ còn mã vật phẩm. Kiểm trên chỉ tiêu giả (khung) và trên chỉ tiêu chi phí thật của 010 (2026-11-11).
 */
@AutoConfigureMockMvc
@Import({TestScopedMetrics.class, FakeFileConfig.class})
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

    private static final String MEDICATION_NAME = "Amlodipin";

    private void medicationCost(YearMonth month, String itemCode) {
        long period = TestUsers.nextId();
        jdbc.update(
                """
                INSERT INTO cost_statement(id,kind,period_month,period_starts_on,period_ends_on,status,unique_key,created_at)
                VALUES (?,'PERIOD',?,?,?,'AWAITING_FINALIZATION',?,?)
                """,
                period,
                month.toString(),
                month.atDay(1),
                month.atEndOfMonth(),
                "PERIOD:" + month,
                Timestamp.from(time.now()));
        medicationCost(month, period, itemCode);
        medicationCost(month, period, itemCode);
    }

    private void medicationCost(YearMonth month, long period, String itemCode) {
        String code = UUID.randomUUID().toString().substring(0, 20);
        jdbc.update(
                "INSERT INTO resident (profile_code, full_name, date_of_birth, gender, status, status_since,"
                        + " created_at, created_by) VALUES (?, 'Người cao tuổi SC-005', '1940-05-01', 'FEMALE',"
                        + " 'IN_RESIDENCE', NOW(6), NOW(6), 1)",
                code);
        long resident = jdbc.queryForObject("SELECT id FROM resident WHERE profile_code = ?", Long.class, code);
        long statement = TestUsers.nextId(), item = TestUsers.nextId();
        Timestamp now = Timestamp.from(time.now());
        jdbc.update("""
                INSERT INTO cost_statement(id,kind,period_id,resident_id,statement_type,unique_key,created_at)
                VALUES (?,'STATEMENT',?,?,'REGULAR',?,?)
                """, statement, period, resident, "STATEMENT:" + period + ":" + resident + ":REGULAR", now);
        jdbc.update(
                """
                INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,source_type,source_id,
                  billing_unit,description,item_code,incurred_on,quantity,unit,unit_price,amount,flags,unique_key,
                  created_at,created_by)
                VALUES (?,?,?,?,'MEDICATION','EVENT_BASED','CARE_TASK_RESULT',?,'UNIT',?,?,?,30,'viên',1000,30000,'',?,?,1)
                """,
                item,
                resident,
                statement,
                period,
                "sc005-" + item,
                MEDICATION_NAME + " 5 mg, 30 viên",
                itemCode,
                month.atDay(2),
                "CARE_TASK_RESULT:sc005-" + item + ":UNIT",
                now);
    }
}
