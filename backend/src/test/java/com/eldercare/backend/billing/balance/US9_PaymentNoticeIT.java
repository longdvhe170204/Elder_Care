package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.billing.api.dto.BalanceDocumentType;
import com.eldercare.backend.billing.api.dto.CostItemView;
import com.eldercare.backend.billing.api.dto.CostStatementType;
import com.eldercare.backend.billing.api.dto.CostStatementView;
import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.billing.api.dto.CostTypeTotal;
import com.eldercare.backend.billing.api.events.BalanceDocumentIssued;
import com.eldercare.backend.billing.api.events.CostStatementFinalized;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData.Line;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData.PaymentInstructions;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData.TypeTotal;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeRecords;
import com.eldercare.backend.billing.balance.internal.document.domain.StatementType;
import com.eldercare.backend.billing.balance.internal.document.service.PaymentNoticeService;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.status.domain.BalanceNotice;
import com.eldercare.backend.billing.internal.provision.domain.FinalizedCostStatement;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 017 T034, T035 (US9; FR-031, FR-032, FR-037, FR-038; BR-M11-21, 23; DBR-37; quickstart Q25 → Q28): một ca IT cho phần
 * của phiếu báo phí không phụ thuộc 010. Lệnh phát hành nhận số liệu đã chụp (nguồn bảng Đã chốt của 010 chờ {@code
 * CostForBalancePort.finalizedStatement}, team-plan mục 8): phát hành lần hai 409; PDF và Excel cùng số liệu; tải lại
 * cùng byte, mỗi lần tải ghi nhật ký; danh sách theo hồ sơ; cổng người thân chỉ cho người có quyền xem chi phí.
 */
class US9_PaymentNoticeIT extends BalanceTestBase {

    @Autowired
    PaymentNoticeService notices;

    @Test
    void Q26_Q28_issueOnce_filesCarrySameFigures_downloadLogged_portalByRight() throws Exception {
        long a = sentForSignature(TestUsers.nextId(), null);
        long statement = TestUsers.nextId();
        String code = PaymentNoticeRecords.code(YearMonth.of(2026, 10), statement);
        PaymentNoticeData data = data(code, -1_250_000);

        as(office);
        assertThatThrownBy(() -> notices.publish(BalanceQueries.target(a), a, statement, data, null))
                .as("Hành chính không phát hành được")
                .isInstanceOf(RuntimeException.class);

        as(accountant);
        var notice = notices.publish(BalanceQueries.target(a), a, statement, data, "00020101021138530010A000000727");
        assertThat(notice.noticeCode()).isEqualTo(code);
        assertThat(notice.balanceAfter()).isEqualTo(-1_250_000);
        assertThatThrownBy(() -> notices.publish(BalanceQueries.target(a), a, statement, data, null))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("ALREADY_ISSUED"));
        assertThat(events.stream(BalanceDocumentIssued.class))
                .contains(new BalanceDocumentIssued(a, BalanceDocumentType.PAYMENT_NOTICE, notice.id()));
        assertThat(events.stream(BalanceNotice.class)
                        .flatMap(n -> n.notificationRequests().stream())
                        .filter(r -> r.eventType().equals("M11_PAYMENT_NOTICE_ISSUED")))
                .singleElement()
                .satisfies(r -> assertThat(r.recipientGroups())
                        .containsExactly(new RecipientGroup.FamilyWithRight(a, FamilyRight.VIEW_COSTS)));

        // danh sách theo hồ sơ
        getAs(accountant, "/api/v1/residents/" + a + "/payment-notices")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].noticeCode").value(code))
                .andExpect(jsonPath("$[0].statementType").value("REGULAR"))
                .andExpect(jsonPath("$[0].balanceBefore").value(8_000_000));
        getAs(nurse, "/api/v1/residents/" + a + "/payment-notices").andExpect(status().isForbidden());

        // PDF và Excel cùng số liệu; tải lại cùng byte; mỗi lần tải ghi nhật ký
        byte[] pdf = download("/api/v1/payment-notices/" + notice.id() + "/file", accountant);
        String text = DocumentReaders.pdfText(pdf);
        assertThat(text)
                .contains(code, "9.250.000", "8.000.000", "-1.250.000", "Số còn nợ: 1.250.000")
                .contains("NCT7K2M9X4A", "0011004000123")
                .doesNotContain("Paracetamol");
        byte[] xlsx = download("/api/v1/payment-notices/" + notice.id() + "/file?format=XLSX", manager);
        assertThat(DocumentReaders.excelCells(xlsx)).contains("9250000", "8000000", "-1250000");
        assertThat(String.join("|", DocumentReaders.excelCells(xlsx))).contains(code, "NCT7K2M9X4A");
        assertThat(download("/api/v1/payment-notices/" + notice.id() + "/file", accountant))
                .isEqualTo(pdf);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'BALANCE_DOCUMENT_DOWNLOADED' AND entity_id = ?",
                        Integer.class,
                        Long.toString(notice.id())))
                .isEqualTo(3);
        getAs(nurse, "/api/v1/payment-notices/" + notice.id() + "/file").andExpect(status().isForbidden());

        // cổng người thân (FR-038): có quyền xem chi phí thì thấy và tải; không có thì 403
        long withCosts = TestUsers.nextId();
        long withoutCosts = TestUsers.nextId();
        relationships.relate(withCosts, a);
        relationships.relate(withoutCosts, a);
        relationships.disable(withoutCosts, a, FamilyRight.VIEW_COSTS);
        var viewer = users.family(withCosts);
        var blocked = users.family(withoutCosts);
        String portal = "/api/v1/family-portal/residents/" + a + "/documents";
        getAs(blocked, portal).andExpect(status().isForbidden());
        getAs(viewer, portal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("PAYMENT_NOTICE"))
                .andExpect(jsonPath("$[0].code").value(code))
                .andExpect(jsonPath("$[0].formats[1]").value("XLSX"));
        String file = "/api/v1/family-portal/documents/PAYMENT_NOTICE/" + notice.id() + "/file";
        assertThat(download(file, viewer)).isEqualTo(pdf);
        getAs(blocked, file).andExpect(status().isForbidden());
    }

    @Test
    void Q25_Q27_pendingList_issueFromFinalizedStatement_balancesMatchLedger() throws Exception {
        long a = sentForSignature(TestUsers.nextId(), null);
        postAs(
                        accountant,
                        "/api/v1/residents/" + a + "/cash-payments",
                        "{\"ledger\":\"BALANCE\",\"amount\":8000000,\"payerName\":\"Lê Văn Hải\",\"receivedAt\":\""
                                + time.now() + "\"}")
                .andExpect(status().isCreated());
        long statement = TestUsers.nextId();
        long notPaid = TestUsers.nextId();
        as(accountant);
        tx.executeWithoutResult(s -> publisher.publishEvent(new CostStatementFinalized(
                statement, CostStatementType.REGULAR, TestUsers.nextId(), a, 9_250_000, time.now(), List.of())));
        costs.statement(finalizedStatement(statement, a, false));
        costs.statement(finalizedStatement(notPaid, a, false));

        configureCollectionAccount();
        try {
            // danh sách chờ: chỉ Kế toán, bảng đã có giao dịch thanh toán
            String pending = "/api/v1/payment-notices/pending-issue";
            getAs(nurse, pending).andExpect(status().isForbidden());
            String body = getAs(accountant, pending)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            List<java.util.Map<String, Object>> mine =
                    JsonPath.read(body, "$[?(@.costStatementId == " + statement + ")]");
            assertThat(mine).as(body).hasSize(1);
            assertThat(mine.getFirst())
                    .containsEntry("residentId", (int) a)
                    .containsEntry("period", "10/2026")
                    .containsEntry("statementType", "REGULAR")
                    .containsEntry("total", 9_250_000);
            assertThat(JsonPath.<List<Object>>read(body, "$[?(@.costStatementId == " + notPaid + ")]"))
                    .isEmpty();

            // phát hành: Hành chính 403; bảng chưa có giao dịch thanh toán 409; không có bảng 404
            String issue = "/api/v1/costs/statements/" + statement + "/payment-notice";
            postAs(office, issue, "").andExpect(status().isForbidden());
            postAs(accountant, "/api/v1/costs/statements/" + notPaid + "/payment-notice", "")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("STATEMENT_NOT_PAID"));
            postAs(accountant, "/api/v1/costs/statements/" + TestUsers.nextId() + "/payment-notice", "")
                    .andExpect(status().isNotFound());
            postAs(accountant, issue, "")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.noticeCode")
                            .value(PaymentNoticeRecords.code(YearMonth.of(2026, 10), statement)))
                    .andExpect(jsonPath("$.costStatementId").value(statement))
                    .andExpect(jsonPath("$.total").value(9_250_000))
                    .andExpect(jsonPath("$.balanceBefore").value(8_000_000))
                    .andExpect(jsonPath("$.balanceAfter").value(-1_250_000));
            postAs(accountant, issue, "")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("ALREADY_ISSUED"));
            getAs(accountant, pending)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.costStatementId == " + statement + ")]")
                            .isEmpty());

            // số liệu trong PDF lấy từ bảng đã chốt: khoản thuốc không tên thuốc, tổng theo loại, số còn nợ, mã nộp
            // tiền
            long noticeId = jdbc.queryForObject(
                    "SELECT id FROM record_event WHERE unique_key = ?",
                    Long.class,
                    PaymentNoticeRecords.key(statement));
            String text =
                    DocumentReaders.pdfText(download("/api/v1/payment-notices/" + noticeId + "/file", accountant));
            assertThat(text)
                    .contains("Phí lưu trú", "9.000.000", "Thuốc MED-1", "250.000", "Số còn nợ: 1.250.000")
                    // CFG-M11-10: còn nợ thì phiếu ghi mã nộp tiền, ngân hàng, tài khoản thu
                    .contains("Vietcombank", "0011004000123")
                    .doesNotContain("Liên hệ Kế toán để biết tài khoản nộp tiền")
                    .doesNotContain("Paracetamol");
        } finally {
            restoreCollectionAccount();
        }
    }

    private static final String CFG = "/api/v1/parameters/CFG-M11-10";

    private void configureCollectionAccount() throws Exception {
        as(manager);
        String etag = mvc.perform(get(CFG)).andReturn().getResponse().getHeader("ETag");
        mvc.perform(
                        post(CFG + "/change-value")
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.csrf())
                                .header("If-Match", etag)
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"value\":{\"bin\":\"970436\",\"accountNumber\":\"0011004000123\",\"accountHolder\":\"Viện A\"},\"reason\":\"Tài khoản thu của viện\"}"))
                .andExpect(status().isOk());
    }

    private void restoreCollectionAccount() throws Exception {
        as(manager);
        String etag = mvc.perform(get(CFG)).andReturn().getResponse().getHeader("ETag");
        mvc.perform(
                        post(CFG + "/change-value")
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.csrf())
                                .header("If-Match", etag)
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"restoreDefault\":true,\"reason\":\"Trả lại sau test\"}"))
                .andExpect(status().isOk());
    }

    private static FinalizedCostStatement finalizedStatement(long id, long resident, boolean finalPeriod) {
        var rent = item(1, CostType.RESIDENCY_FEE, "Phí lưu trú tháng 10", null, 9_000_000);
        var medicine = item(2, CostType.MEDICATION, "Thuốc MED-1", "MED-1", 250_000);
        return new FinalizedCostStatement(
                1,
                java.time.Instant.parse("2026-11-01T01:00:00Z"),
                new CostStatementView(
                        id,
                        "2026-10",
                        resident,
                        CostStatementType.REGULAR,
                        finalPeriod,
                        LocalDate.of(2026, 10, 31),
                        "FINALIZED",
                        List.of(
                                new CostTypeTotal(CostType.RESIDENCY_FEE, 9_000_000),
                                new CostTypeTotal(CostType.MEDICATION, 250_000)),
                        9_250_000L,
                        List.of(rent, medicine)));
    }

    private static CostItemView item(long id, CostType type, String description, String code, long amount) {
        return new CostItemView(
                id,
                type,
                "AUTO",
                null,
                description,
                code,
                null,
                LocalDate.of(2026, 10, 31),
                BigDecimal.ONE,
                "tháng",
                amount,
                java.util.Map.of(),
                null,
                amount,
                List.of(),
                "FINALIZED",
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                amount,
                List.of());
    }

    private byte[] download(String url, TestUsers.TestUser who) throws Exception {
        as(who);
        return mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
    }

    private static PaymentNoticeData data(String code, long balanceAfter) {
        return new PaymentNoticeData(
                code,
                java.time.LocalDate.of(2026, 11, 3),
                "Trần Thị Kế Toán",
                "Nguyễn Thị Ánh",
                null,
                "10/2026",
                StatementType.REGULAR,
                List.of(new Line(
                        java.time.LocalDate.of(2026, 10, 31),
                        "Phí lưu trú",
                        "Phí lưu trú tháng 10",
                        BigDecimal.ONE,
                        9_000_000,
                        9_000_000)),
                List.of(new TypeTotal("Phí lưu trú", 9_000_000), new TypeTotal("Thuốc", 250_000)),
                9_250_000,
                8_000_000,
                balanceAfter,
                new PaymentInstructions("Vietcombank", "0011004000123", "VIEN DUONG LAO BINH AN", "NCT7K2M9X4A"));
    }
}
