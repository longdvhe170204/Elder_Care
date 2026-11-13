package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.billing.internal.export.service.CostAccountingExport;
import com.eldercare.backend.billing.internal.item.domain.CostItemSelection;
import com.eldercare.backend.billing.internal.item.service.CostItemService;
import com.eldercare.backend.billing.internal.period.service.CostStatementService;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.YearMonth;
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

/**
 * SC-007, FR-055, FR-052, FR-051 (016 T019; quickstart Q18, Q19): bảng chi phí tháng "10" của A, B, C đã chốt bằng lệnh
 * thật của 010, của D chưa chốt; hai khoản nhập tay; khoản điều chỉnh của tháng "9" nằm trong bảng tháng 10 của A. Ở chế độ
 * đã chốt, {@code COST.SUMMARY} của tháng 10 bằng tổng các bảng Đã chốt và dòng tổng kiểm soát của file kế toán; tháng 9 đã
 * chốt không đổi. Chế độ tạm tính thêm khoản của D và mang nhãn tạm tính; khoản điều chỉnh có kỳ gốc. Hai tháng là tháng
 * riêng của lần chạy (năm 1000+) để không lẫn khoản của test khác.
 */
@AutoConfigureMockMvc
@Import(SwitchableUserConfig.class)
class SC_007_KhopBangChotIT extends MySqlIntegrationTestBase {

    private static final String SUMMARY = "$.values[?(@.metricCode == 'COST.SUMMARY')]";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser current;

    @Autowired
    CostItemService items;

    @Autowired
    CostStatementService statements;

    @Autowired
    CostAccountingExport exports;

    TestUsers users;
    TestUser office, manager, accountant;
    YearMonth september, october;
    long septemberPeriod, octoberPeriod;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        office = users.staff(Role.OFFICE_STAFF);
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        accountant = users.staff(Role.ACCOUNTANT);
        long seed = Math.floorMod(TestUsers.nextId(), 11000);
        september = YearMonth.of(1000 + (int) seed / 12, 1 + (int) seed % 12);
        october = september.plusMonths(1);
        septemberPeriod = period(september);
        octoberPeriod = period(october);
    }

    @AfterEach
    void reset() {
        current.reset();
    }

    @Test
    void SC_007_finalizedModeMatchesFinalizedStatementsAndAccountingControlTotal() throws Exception {
        long a = resident(), b = resident(), c = resident(), d = resident();
        long septemberA = statement(septemberPeriod, a);
        long septemberItem = item(septemberA, septemberPeriod, a, september, "EVENT_BASED", 1000, null);
        approveAndFinalize(septemberA, septemberItem);

        long octoberA = statement(octoberPeriod, a);
        long octoberB = statement(octoberPeriod, b);
        long octoberC = statement(octoberPeriod, c);
        long octoberD = statement(octoberPeriod, d);
        long event = item(octoberA, octoberPeriod, a, october, "EVENT_BASED", 300, null);
        long adjustment = item(octoberA, octoberPeriod, a, october, "ADJUSTMENT", 80, septemberItem);
        approveAndFinalize(octoberA, event, adjustment);
        approveAndFinalize(octoberB, item(octoberB, octoberPeriod, b, october, "MANUAL", 200, null));
        approveAndFinalize(octoberC, item(octoberC, octoberPeriod, c, october, "MANUAL", 150, null));
        current.set(office.user());
        items.check(selection(item(octoberD, octoberPeriod, d, october, "EVENT_BASED", 50, null)));

        long finalizedTotal = jdbc.queryForObject(
                "SELECT SUM(total) FROM cost_statement WHERE period_id=? AND status='FINALIZED'",
                Long.class,
                octoberPeriod);
        assertThat(finalizedTotal).isEqualTo(730);
        assertThat(summary(october, null)).isEqualByComparingTo(BigDecimal.valueOf(finalizedTotal));

        current.set(accountant.user());
        String file = new String(exports.export(octoberPeriod, "ALL", "CSV").bytes(), StandardCharsets.UTF_8);
        assertThat(file).contains("Tổng kiểm soát: 3 bảng; 4 khoản; " + finalizedTotal + " đồng");

        assertThat(summary(september, null)).isEqualByComparingTo("1000");
    }

    private BigDecimal summary(YearMonth month, String costMode) throws Exception {
        List<Number> values = JsonPath.read(report(month, costMode), SUMMARY + ".value");
        return new BigDecimal(values.getFirst().toString());
    }

    private String report(YearMonth month, String costMode) throws Exception {
        var request = get("/api/v1/reports/INCURRED_COSTS")
                .param("from", month.atDay(1).toString())
                .param("to", month.atEndOfMonth().toString());
        if (costMode != null) request.param("costMode", costMode);
        return body(request);
    }

    private String body(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        current.set(manager.user());
        return mvc.perform(request.header("Authorization", TestUsers.bearer(users.session(manager.accountId()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private void approveAndFinalize(long statement, long... ids) {
        current.set(office.user());
        items.check(selection(ids));
        current.set(manager.user());
        for (long id : ids) items.approve(selection(id), null);
        current.set(office.user());
        statements.submit(statement);
        current.set(manager.user());
        statements.finalizeStatement(
                statement, "\"" + statements.read(statement).version() + "\"");
    }

    private static CostItemSelection selection(long... ids) {
        return new CostItemSelection(java.util.Arrays.stream(ids).boxed().toList(), null);
    }

    private long resident() {
        String code = UUID.randomUUID().toString().substring(0, 20);
        jdbc.update(
                "INSERT INTO resident (profile_code, full_name, date_of_birth, gender, status, status_since,"
                        + " created_at, created_by) VALUES (?, 'Người cao tuổi SC-007', '1940-05-01', 'FEMALE',"
                        + " 'IN_RESIDENCE', NOW(6), NOW(6), 1)",
                code);
        return jdbc.queryForObject("SELECT id FROM resident WHERE profile_code = ?", Long.class, code);
    }

    private long period(YearMonth month) {
        long id = TestUsers.nextId();
        jdbc.update("""
                INSERT INTO cost_statement(id,kind,period_month,period_starts_on,period_ends_on,
                  status,unique_key,created_at)
                VALUES (?,'PERIOD',?,?,?,'AWAITING_FINALIZATION',?,?)
                """, id, month.toString(), month.atDay(1), month.atEndOfMonth(), "PERIOD:" + month, now());
        return id;
    }

    private long statement(long period, long resident) {
        long id = TestUsers.nextId();
        jdbc.update("""
                INSERT INTO cost_statement(id,kind,period_id,resident_id,statement_type,unique_key,created_at)
                VALUES (?,'STATEMENT',?,?,'REGULAR',?,?)
                """, id, period, resident, "STATEMENT:" + period + ":" + resident + ":REGULAR", now());
        return id;
    }

    /** Khoản như lệnh lập của 010 ghi; điều chỉnh mang kỳ gốc của khoản gốc (DBR-17). */
    private long item(
            long statement, long period, long resident, YearMonth month, String origin, long amount, Long original) {
        long id = TestUsers.nextId();
        boolean event = origin.equals("EVENT_BASED");
        String source = "sc007-" + id;
        jdbc.update(
                """
                INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,
                  source_type,source_id,billing_unit,description,incurred_on,quantity,unit,unit_price,
                  amount,flags,unique_key,reason,original_item_id,original_period_id,adjustment_type,
                  created_at,created_by)
                VALUES (?,?,?,?,'SERVICE',?,?,?,?,'Dịch vụ SC-007',?,1,'UNIT',?,?,'',?,'Kiểm thử SC-007',?,?,?,?,?)
                """,
                id,
                resident,
                statement,
                period,
                origin,
                event ? "CARE_TASK_RESULT" : null,
                event ? source : null,
                event ? "UNIT" : null,
                month.atDay(2),
                amount,
                amount,
                event ? "CARE_TASK_RESULT:" + source + ":UNIT" : null,
                original,
                original == null ? null : septemberPeriod,
                original == null ? null : "POST_FINALIZATION_CORRECTION",
                now(),
                office.accountId());
        return id;
    }

    private Timestamp now() {
        return Timestamp.from(time.now());
    }
}
