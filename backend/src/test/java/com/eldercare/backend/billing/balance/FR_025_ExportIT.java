package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.billing.api.dto.CostStatementType;
import com.eldercare.backend.billing.api.events.CostStatementFinalized;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.support.TestUsers;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 017 T037, T038 (US8; FR-025 → FR-027; quickstart Q29 → Q33): một ca IT cho luồng chính; tổng của báo cáo thu chi và
 * bảo đảm đầu kỳ + giao dịch = cuối kỳ ở {@link BalanceStatementTest}, {@link IncomeExpenseReportTest}. Sao kê của hai
 * hồ sơ ra Excel và PDF cùng số liệu; file lưu một lần, tải lại cùng byte dù sổ đổi; báo cáo thu chi liệt kê người còn
 * nợ; khoảng quá CFG-M14-01 là 422; Hành chính 403, Quản lý viện chỉ tải; cổng người thân theo quyền xem chi phí.
 */
class FR_025_ExportIT extends BalanceTestBase {

    @Test
    void Q29_Q33_statementAndReport_storedOnce_sameFigures_rangeLimited_portalByRight() throws Exception {
        long a = sentForSignature(TestUsers.nextId(), null);
        long b = sentForSignature(TestUsers.nextId(), null);
        cash(a, 4_000_000);
        cash(b, 1_000_000);
        finalized(b, 1_700_000);
        LocalDate today = time.today();
        String range = "\"from\":\"" + today.minusDays(1) + "\",\"to\":\"" + today + "\"";

        // Hành chính 403; Quản lý viện xem được nhưng không lập lệnh xuất
        export(office, "statement", "{\"residentIds\":[" + a + "]," + range + ",\"format\":\"PDF\"}")
                .andExpect(status().isForbidden());
        export(manager, "statement", "{\"residentIds\":[" + a + "]," + range + ",\"format\":\"PDF\"}")
                .andExpect(status().isForbidden());
        // khoảng quá CFG-M14-01 (12 tháng)
        export(
                        accountant,
                        "statement",
                        "{\"residentIds\":[" + a + "],\"from\":\"" + today.minusYears(2) + "\",\"to\":\"" + today
                                + "\",\"format\":\"XLSX\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("EXPORT_RANGE_EXCEEDED"));

        // Excel và PDF cùng số liệu
        String body = "{\"residentIds\":[" + a + "," + b + "]," + range + ",\"format\":\"%s\"}";
        long xlsxId = id(export(accountant, "statement", body.formatted("XLSX"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("BALANCE_STATEMENT"))
                .andExpect(jsonPath("$.format").value("XLSX"))
                .andExpect(jsonPath("$.rowCount").value(3)));
        long pdfId = id(export(accountant, "statement", body.formatted("PDF")).andExpect(status().isCreated()));
        byte[] xlsx = download(xlsxId, accountant);
        byte[] pdf = download(pdfId, manager);
        assertThat(String.join("|", DocumentReaders.excelCells(xlsx))).contains("4000000", "1000000", "-700000");
        assertThat(DocumentReaders.pdfText(pdf)).contains("4.000.000", "1.000.000", "-700.000");

        // file không đổi khi sổ đổi (SC-009); mỗi lần xuất, tải ghi nhật ký
        cash(a, 500_000);
        assertThat(download(xlsxId, accountant)).isEqualTo(xlsx);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'BALANCE_DOCUMENT_DOWNLOADED' AND entity_id = ?",
                        Integer.class,
                        Long.toString(xlsxId)))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'BALANCE_EXPORTED' AND entity_id = ?",
                        Integer.class,
                        Long.toString(xlsxId)))
                .isEqualTo(1);
        as(office);
        mvc.perform(get("/api/v1/balance-exports/" + xlsxId + "/file")).andExpect(status().isForbidden());

        // báo cáo thu chi: hồ sơ b còn nợ 700.000 đồng
        long reportId = id(export(accountant, "income-expense-report", "{" + range + ",\"format\":\"XLSX\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INCOME_EXPENSE")));
        assertThat(String.join("|", DocumentReaders.excelCells(download(reportId, accountant))))
                .contains(residents.identity(b).fullName(), "700000");

        // cổng người thân: có quyền xem chi phí thì tải, không có thì 403, khoảng quá hạn 422
        long withCosts = TestUsers.nextId();
        long withoutCosts = TestUsers.nextId();
        relationships.relate(withCosts, a);
        relationships.relate(withoutCosts, a);
        relationships.disable(withoutCosts, a, FamilyRight.VIEW_COSTS);
        var viewer = users.family(withCosts);
        var blocked = users.family(withoutCosts);
        String portal = "/api/v1/family-portal/residents/" + a + "/balance-statement";
        String portalBody = "{" + range + ",\"format\":\"PDF\"}";
        as(blocked);
        portalPost(portal, portalBody).andExpect(status().isForbidden());
        as(viewer);
        byte[] portalFile = portalPost(portal, portalBody)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        assertThat(DocumentReaders.pdfText(portalFile)).contains("4.500.000");
        portalPost(portal, "{\"from\":\"" + today.minusYears(2) + "\",\"to\":\"" + today + "\",\"format\":\"PDF\"}")
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'BALANCE_EXPORTED' AND resident_id = ?",
                        Integer.class,
                        a))
                .isEqualTo(1);
    }

    private ResultActions export(TestUsers.TestUser who, String path, String json) throws Exception {
        as(who);
        return mvc.perform(post("/api/v1/balance-exports/" + path)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions portalPost(String url, String json) throws Exception {
        return mvc.perform(post(url)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private byte[] download(long id, TestUsers.TestUser who) throws Exception {
        as(who);
        return mvc.perform(get("/api/v1/balance-exports/" + id + "/file"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
    }

    private long id(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void cash(long resident, long amount) throws Exception {
        postAs(
                        accountant,
                        "/api/v1/residents/" + resident + "/cash-payments",
                        "{\"ledger\":\"BALANCE\",\"amount\":" + amount
                                + ",\"payerName\":\"Lê Văn Hải\",\"receivedAt\":\"" + time.now() + "\"}")
                .andExpect(status().isCreated());
    }

    private void finalized(long resident, long total) {
        as(accountant);
        tx.executeWithoutResult(s -> publisher.publishEvent(new CostStatementFinalized(
                TestUsers.nextId(),
                CostStatementType.REGULAR,
                TestUsers.nextId(),
                resident,
                total,
                time.now(),
                List.of())));
    }
}
