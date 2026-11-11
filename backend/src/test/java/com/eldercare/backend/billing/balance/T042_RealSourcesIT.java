package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.events.ContractStatusChanged;
import com.eldercare.backend.billing.internal.item.domain.CostItemSelection;
import com.eldercare.backend.billing.internal.item.service.CostItemService;
import com.eldercare.backend.billing.internal.period.service.CostStatementService;
import com.eldercare.backend.billing.internal.provision.domain.CostForBalancePort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.RealCareFixture;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 017 T042 (chạy lại US1, US2, US5, US9 với bean thật thay T006, 2026-11-11): không có {@code FakeM02Config},
 * {@code FakeM11Config}; hợp đồng thật của 004 (sổ mở khi hợp đồng gửi ký, đọc cọc qua {@code ContractReadApi} thật), bảng
 * chi phí thật của 010 chốt bằng lệnh thật phát {@code CostStatementFinalized}, 017 trừ vào số dư; chi phí chưa chốt đọc
 * qua {@code CostForBalance} thật.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import({SwitchableUserConfig.class, com.eldercare.backend.support.fake.file.FakeFileConfig.class})
class T042_RealSourcesIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    CostItemService items;

    @Autowired
    CostStatementService statements;

    @Autowired
    CostForBalancePort costs;

    @Autowired
    com.eldercare.backend.billing.balance.internal.document.service.PaymentNoticeIssuance notices;

    @AfterEach
    void restore() {
        user.reset();
    }

    @Autowired
    com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore relationshipHistory;

    /**
     * US6 #2, #8: hoàn tiền cho người đại diện thật của 012 ({@code RepresentativeLookup}): Chờ duyệt, Quản lý viện duyệt
     * thì Đã xác nhận, số dư giảm; quan hệ người đại diện kết thúc trước lúc duyệt thì duyệt bị chặn.
     */
    @Test
    void US6_refundToRealRepresentative_approvedThenBlockedWhenRepresentationEnds() throws Exception {
        var fixture = new RealCareFixture(jdbc, time);
        TestUser accountant = fixture.users().staff(Role.ACCOUNTANT);
        TestUser office = fixture.users().staff(Role.OFFICE_STAFF);
        TestUser manager = fixture.users().staff(Role.NURSING_HOME_MANAGER);
        long resident = fixture.residentInBed(fixture.place(), time.now().minus(Duration.ofDays(3)));
        long contract = jdbc.queryForObject("SELECT id FROM contract WHERE resident_id = ?", Long.class, resident);
        user.set(office.user());
        tx.executeWithoutResult(s -> publisher.publishEvent(new ContractStatusChanged(
                contract, resident, ContractStatus.DRAFT, ContractStatus.PENDING_SIGNATURE, null)));
        var relationships = new com.eldercare.backend.family.RelationshipFixtures(
                jdbc, tx, relationshipHistory, time.now().minus(Duration.ofDays(10)));
        long representative = relationships.familyMember("Trần Thị Đại Diện", "0901000777");
        long relationship = relationships.relate(representative, resident, 1, true, true);

        user.set(accountant.user());
        mvc.perform(json(
                        "/api/v1/residents/" + resident + "/cash-payments",
                        "{\"ledger\":\"BALANCE\",\"amount\":2000000,\"payerName\":\"Lê Văn Hải\",\"receivedAt\":\""
                                + time.now() + "\"}"))
                .andExpect(status().isCreated());
        long first = transaction(refund(resident, 1_500_000, representative));
        assertThat(statusOf(first)).isEqualTo("PENDING_APPROVAL");
        decide(manager, requestOf(first)).andExpect(status().isOk());
        assertThat(statusOf(first)).isEqualTo("CONFIRMED");
        assertThat(balance(resident)).isEqualByComparingTo("500000");

        user.set(accountant.user());
        long second = transaction(refund(resident, 500_000, representative));
        relationships.end(relationship, time.now());
        decide(manager, requestOf(second))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("RECIPIENT_NO_LONGER_REPRESENTATIVE"));
        assertThat(statusOf(second)).isEqualTo("PENDING_APPROVAL");
    }

    /**
     * US7 #1, US10 #2: hồ sơ kết thúc lưu trú lập bằng lệnh thật của 004 (lập bảng kỳ cuối nháp ở 010 thật, phát
     * {@code ResidencyEndingCreated}); quyết toán của 017 đọc điều kiện kỳ cuối qua {@code FinalPeriodCostApi} thật: chưa
     * chốt thì điều kiện tiền chưa thỏa và không xác nhận quyết toán được.
     */
    @Test
    void US7_realEndingAndFinalPeriod_settlementBlockedUntilFinalPeriodFinalized() throws Exception {
        var fixture = new RealCareFixture(jdbc, time);
        TestUser accountant = fixture.users().staff(Role.ACCOUNTANT);
        TestUser office = fixture.users().staff(Role.OFFICE_STAFF);
        TestUser manager = fixture.users().staff(Role.NURSING_HOME_MANAGER);
        long resident = fixture.residentInBed(fixture.place(), time.now().minus(Duration.ofDays(20)));
        long contract = jdbc.queryForObject("SELECT id FROM contract WHERE resident_id = ?", Long.class, resident);
        user.set(office.user());
        tx.executeWithoutResult(s -> publisher.publishEvent(new ContractStatusChanged(
                contract, resident, ContractStatus.DRAFT, ContractStatus.PENDING_SIGNATURE, null)));
        user.set(accountant.user());
        mvc.perform(json(
                        "/api/v1/residents/" + resident + "/cash-payments",
                        "{\"ledger\":\"BALANCE\",\"amount\":4000000,\"payerName\":\"Lê Văn Hải\",\"receivedAt\":\""
                                + time.now() + "\"}"))
                .andExpect(status().isCreated());

        user.set(office.user());
        mvc.perform(json(
                        "/api/v1/residency-endings",
                        "{\"residentId\":" + resident + ",\"endCase\":\"DISCHARGE\",\"expectedEndDate\":\""
                                + time.today().plusDays(5)
                                + "\",\"requestedBy\":\"Con gái\",\"reason\":\"Gia đình đưa về nhà\"}"))
                .andExpect(status().isCreated());

        user.set(manager.user());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/residents/" + resident + "/settlement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditions[0].code").value("END_H_SETTLEMENT"))
                .andExpect(jsonPath("$.conditions[0].status").value("NOT_MET"));
        user.set(accountant.user());
        mvc.perform(json("/api/v1/residents/" + resident + "/settlement/confirm", ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SETTLEMENT_CONDITIONS_NOT_MET"));
    }

    private static String refund(long resident, long amount, long recipient) {
        return "{\"type\":\"REFUND\",\"residentId\":" + resident + ",\"ledger\":\"BALANCE\",\"amount\":" + amount
                + ",\"recipient\":{\"familyMemberId\":" + recipient + "},\"method\":\"BANK_TRANSFER\","
                + "\"reason\":\"Gia đình xin rút\"}";
    }

    private org.springframework.test.web.servlet.RequestBuilder json(String url, String body) {
        return post(url)
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private long transaction(String body) throws Exception {
        String created = mvc.perform(json("/api/v1/balance-transactions", body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(created, "$.id")).longValue();
    }

    private org.springframework.test.web.servlet.ResultActions decide(TestUser who, long request) throws Exception {
        user.set(who.user());
        String etag = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/approval-requests/" + request))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        return mvc.perform(post("/api/v1/approval-requests/" + request + "/approve")
                .header("If-Match", etag)
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Duyệt\"}"));
    }

    private long requestOf(long transaction) {
        return jdbc.queryForObject(
                "SELECT approval_request_id FROM balance_transaction WHERE id = ?", Long.class, transaction);
    }

    private String statusOf(long transaction) {
        return jdbc.queryForObject("SELECT status FROM balance_transaction WHERE id = ?", String.class, transaction);
    }

    @Test
    void US1_US2_US5_US9_realContractAndFinalizedStatement_deductedFromBalance() throws Exception {
        var fixture = new RealCareFixture(jdbc, time);
        TestUser accountant = fixture.users().staff(Role.ACCOUNTANT);
        TestUser office = fixture.users().staff(Role.OFFICE_STAFF);
        TestUser manager = fixture.users().staff(Role.NURSING_HOME_MANAGER);
        long resident = fixture.residentInBed(fixture.place(), time.now().minus(Duration.ofDays(3)));
        long contract = jdbc.queryForObject("SELECT id FROM contract WHERE resident_id = ?", Long.class, resident);

        // US1: 004 gửi ký hợp đồng thì 017 mở sổ (cọc đọc qua ContractReadApi thật).
        user.set(office.user());
        tx.executeWithoutResult(s -> publisher.publishEvent(new ContractStatusChanged(
                contract, resident, ContractStatus.DRAFT, ContractStatus.PENDING_SIGNATURE, null)));
        assertThat(jdbc.queryForObject(
                        "SELECT opening_contract_id FROM resident_balance WHERE resident_id = ?", Long.class, resident))
                .isEqualTo(contract);

        // US2: nộp tiền mặt vào số dư.
        user.set(accountant.user());
        mvc.perform(post("/api/v1/residents/" + resident + "/cash-payments")
                        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"ledger\":\"BALANCE\",\"amount\":5000000,\"payerName\":\"Lê Văn Hải\",\"receivedAt\":\""
                                        + time.now() + "\"}"))
                .andExpect(status().isCreated());
        assertThat(balance(resident)).isEqualByComparingTo("5000000");

        // 010 thật: một khoản chưa chốt, chốt bảng bằng lệnh thật; 017 trừ tổng bảng vào số dư.
        long seed = Math.floorMod(TestUsers.nextId(), 11000);
        YearMonth month = YearMonth.of(1000 + (int) seed / 12, 1 + (int) seed % 12);
        long period = TestUsers.nextId(), statement = TestUsers.nextId(), item = TestUsers.nextId();
        Timestamp now = Timestamp.from(time.now());
        jdbc.update("""
                INSERT INTO cost_statement(id,kind,period_month,period_starts_on,period_ends_on,status,unique_key,created_at)
                VALUES (?,'PERIOD',?,?,?,'AWAITING_FINALIZATION',?,?)
                """, period, month.toString(), month.atDay(1), month.atEndOfMonth(), "PERIOD:" + month, now);
        jdbc.update("""
                INSERT INTO cost_statement(id,kind,period_id,resident_id,statement_type,unique_key,created_at)
                VALUES (?,'STATEMENT',?,?,'REGULAR',?,?)
                """, statement, period, resident, "STATEMENT:" + period + ":" + resident + ":REGULAR", now);
        jdbc.update(
                """
                INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,source_type,source_id,
                  billing_unit,description,incurred_on,quantity,unit,unit_price,amount,flags,unique_key,created_at,created_by)
                VALUES (?,?,?,?,'SERVICE','EVENT_BASED','CARE_TASK_RESULT',?,'UNIT','Dịch vụ thật',?,1,'UNIT',1200000,1200000,'',?,?,?)
                """,
                item,
                resident,
                statement,
                period,
                "t042-" + item,
                month.atDay(2),
                "CARE_TASK_RESULT:t042-" + item + ":UNIT",
                now,
                office.accountId());
        assertThat(costs.unfinalizedTotal(resident))
                .as("US5: chi phí chưa chốt từ 010 thật")
                .isEqualTo(1_200_000);

        user.set(office.user());
        items.check(new CostItemSelection(List.of(item), null));
        user.set(manager.user());
        items.approve(new CostItemSelection(List.of(item), null), null);
        user.set(office.user());
        statements.submit(statement);
        user.set(manager.user());
        statements.finalizeStatement(
                statement, "\"" + statements.read(statement).version() + "\"");

        assertThat(costs.finalizedStatement(statement)).isPresent();
        assertThat(costs.unfinalizedTotal(resident)).isZero();
        assertThat(balance(resident)).isEqualByComparingTo("3800000");

        // US9: phiếu báo phí lập từ bảng Đã chốt thật của 010, số dư trước/sau theo giao dịch thanh toán bảng.
        user.set(accountant.user());
        assertThat(notices.pendingIssue()).extracting(n -> n.costStatementId()).contains(statement);
        var notice = notices.issue(statement);
        assertThat(notice.total()).isEqualTo(1_200_000);
        assertThat(notice.balanceBefore()).isEqualTo(5_000_000);
        assertThat(notice.balanceAfter()).isEqualTo(3_800_000);
    }

    private BigDecimal balance(long resident) {
        return jdbc.queryForObject(
                "SELECT balance FROM resident_balance WHERE resident_id = ?", BigDecimal.class, resident);
    }
}
