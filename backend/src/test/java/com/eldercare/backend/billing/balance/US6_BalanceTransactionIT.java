package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.dto.EndCase;
import com.eldercare.backend.admission.api.dto.ResidencyEndingView;
import com.eldercare.backend.admission.api.dto.SettlementInfoView;
import com.eldercare.backend.billing.api.dto.CostStatementType;
import com.eldercare.backend.billing.api.dto.FinalPeriodConditionView;
import com.eldercare.backend.billing.api.events.CostStatementFinalized;
import com.eldercare.backend.billing.balance.internal.status.service.DepositRefundReminders;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.admission.FakeRepresentatives;
import com.eldercare.backend.support.fake.admission.FakeResidencyEndingReads;
import com.eldercare.backend.support.fake.billing.FakeFinalPeriodCosts;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 017 T024 (US6 #1 → #8; FR-010 → FR-015; BR-M11-10, 14; DBR-17; quickstart Q11 → Q15): một ca IT cho luồng chính của
 * Phase 7 (ngân sách IT, B chọn 2026-11-08). Hoàn tiền, điều chỉnh Chờ duyệt không đổi sổ; duyệt kiểm lại giới hạn
 * (bị chặn thì giữ Chờ duyệt); duyệt thì Đã xác nhận, cập nhật sổ, giao dịch gốc "đã điều chỉnh"; từ chối, hủy; không
 * điều chỉnh thanh toán bảng; danh sách chờ duyệt kèm người cao tuổi; cấn trừ cọc là cặp liên kết khi đủ điều kiện;
 * nhắc hoàn cọc khi hợp đồng bị hủy (FR-015a).
 */
class US6_BalanceTransactionIT extends BalanceTestBase {

    @Autowired
    FakeRepresentatives representatives;

    @Autowired
    FakeResidencyEndingReads endings;

    @Autowired
    FakeFinalPeriodCosts finalPeriods;

    @Autowired
    DepositRefundReminders refunds;

    @Test
    void Q11_Q15_Q24_pendingUntilApproved_recheckedOnApproval_adjustmentMarksOriginal_offsetIsALinkedPair()
            throws Exception {
        long a = sentForSignature(TestUsers.nextId(), null);
        long p = TestUsers.nextId();
        representatives.representative(a, p);
        cash(a, "BALANCE", 2_000_000);

        // US6 #1: vượt số dư bị chặn khi lập
        newTransaction(refund(a, 3_000_000, p))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BALANCE_EXCEEDED"));
        // Hành chính không lập được
        as(office);
        mvc.perform(command("/api/v1/balance-transactions", refund(a, 1_000_000, p)))
                .andExpect(status().isForbidden());

        // US6 #2: Chờ duyệt, sổ chưa đổi
        long first = id(newTransaction(refund(a, 2_000_000, p))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("REFUND"))
                .andExpect(jsonPath("$.amount").value(-2_000_000))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL")));
        assertThat(balance(a)).isEqualTo(2_000_000);
        long firstRequest = requestOf(first);

        // Danh sách chờ duyệt kèm người cao tuổi; Hành chính không xem
        String list = getAs(manager, "/api/v1/balance-transactions/pending-approval")
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<java.util.Map<String, Object>> mine = JsonPath.read(list, "$[?(@.id == " + first + ")]");
        assertThat(mine).as(list).singleElement().satisfies(m -> {
            assertThat(((Number) m.get("residentId")).longValue()).isEqualTo(a);
            assertThat(m.get("residentName")).isEqualTo("Nguyễn Văn " + a);
        });
        getAs(office, "/api/v1/balance-transactions/pending-approval").andExpect(status().isForbidden());

        // US6 #3: bảng 500.000 đồng chốt trước lúc duyệt → duyệt bị chặn, giữ Chờ duyệt
        finalized(TestUsers.nextId(), a, 500_000);
        approve(firstRequest)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("BALANCE_EXCEEDED"));
        assertThat(statusOf(first)).isEqualTo("PENDING_APPROVAL");
        // Kế toán hủy, có lý do
        cancel(firstRequest).andExpect(status().isOk());
        assertThat(statusOf(first)).isEqualTo("CANCELLED");

        // Hoàn 1.500.000 đồng được duyệt: Đã xác nhận, số dư 0
        long second = id(newTransaction(refund(a, 1_500_000, p)).andExpect(status().isCreated()));
        approve(requestOf(second)).andExpect(status().isOk());
        assertThat(statusOf(second)).isEqualTo("CONFIRMED");
        assertThat(balance(a)).isZero();
        assertThat(jdbc.queryForObject("SELECT approved_by FROM balance_transaction WHERE id = ?", Long.class, second))
                .isEqualTo(manager.accountId());

        // US6 #4: điều chỉnh sửa khoản nộp ghi nhầm, giao dịch gốc mang dấu "đã điều chỉnh"
        long wrong = cash(a, "BALANCE", 5_000_000);
        long adjustment = id(newTransaction(adjustment(a, -5_000_000, wrong)).andExpect(status().isCreated()));
        assertThat(balance(a)).isEqualTo(5_000_000);
        approve(requestOf(adjustment)).andExpect(status().isOk());
        assertThat(balance(a)).isZero();
        assertThat(jdbc.queryForObject("SELECT adjusted FROM balance_transaction WHERE id = ?", Boolean.class, wrong))
                .isTrue();

        // US6 #5: không điều chỉnh thanh toán bảng
        long payment = jdbc.queryForObject(
                "SELECT id FROM balance_transaction WHERE resident_id = ? AND type = 'STATEMENT_PAYMENT'",
                Long.class,
                a);
        newTransaction(adjustment(a, 500_000, payment))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATEMENT_PAYMENT_NOT_ADJUSTABLE"));

        // US6 #6: từ chối có lý do, số dư không đổi
        long rejected = id(newTransaction(adjustment(a, 300_000, null)).andExpect(status().isCreated()));
        reject(requestOf(rejected)).andExpect(status().isOk());
        assertThat(statusOf(rejected)).isEqualTo("REJECTED");
        assertThat(balance(a)).isZero();

        // US6 #8: người nhận thôi là người đại diện trước lúc duyệt → duyệt bị chặn
        cash(a, "BALANCE", 1_000_000);
        long third = id(newTransaction(refund(a, 1_000_000, p)).andExpect(status().isCreated()));
        representatives.reset();
        approve(requestOf(third))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("RECIPIENT_NO_LONGER_REPRESENTATIVE"));

        // FR-015 (Q24): cấn trừ cọc chỉ khi có hồ sơ kết thúc Đang chuẩn bị và bảng kỳ cuối đã chốt
        long b = sentForSignature(TestUsers.nextId(), 3_000_000L);
        cash(b, "DEPOSIT", 3_000_000);
        finalized(TestUsers.nextId(), b, 1_000_000);
        finalPeriods.condition(b, new FinalPeriodConditionView(false, List.of("Bảng kỳ cuối chưa chốt")));
        offset(b, 1_000_000)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("OFFSET_CONDITIONS_NOT_MET"));
        endings.preparing(
                new ResidencyEndingView(TestUsers.nextId(), b, EndCase.CONTRACT_EXPIRED, time.today(), null),
                new SettlementInfoView(null, null, null, time.today(), EndCase.CONTRACT_EXPIRED, null));
        finalPeriods.condition(b, new FinalPeriodConditionView(true, List.of()));
        offset(b, 1_500_000)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("OFFSET_CONDITIONS_NOT_MET"));
        String pair = offset(b, 1_000_000)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(balance(b)).isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT deposit_balance FROM resident_balance WHERE resident_id = ?", Long.class, b))
                .isEqualTo(2_000_000);
        List<Integer> ids = JsonPath.read(pair, "$[*].id");
        List<Integer> linked = JsonPath.read(pair, "$[*].linkedTransactionId");
        assertThat(linked).containsExactly(ids.get(1), ids.get(0));

        // FR-015a: hợp đồng Đã hủy trước khi Hiệu lực mà còn cọc → nhắc Kế toán; nhắc lại sau CFG-M02-09
        long contract = TestUsers.nextId();
        long c = sentForSignature(contract, 2_000_000L);
        cash(c, "DEPOSIT", 2_000_000);
        changeContract(contract, c, ContractStatus.PENDING_SIGNATURE, ContractStatus.CANCELLED);
        refunds.remind();
        var remindedAt = refundRemindedAt(c);
        assertThat(remindedAt).isNotNull();
        refunds.remind();
        assertThat(refundRemindedAt(c)).isEqualTo(remindedAt);
    }

    private java.sql.Timestamp refundRemindedAt(long resident) {
        return jdbc.queryForObject(
                "SELECT deposit_refund_reminded_at FROM resident_balance WHERE resident_id = ?",
                java.sql.Timestamp.class,
                resident);
    }

    private String refund(long resident, long amount, long recipient) {
        return "{\"type\":\"REFUND\",\"residentId\":" + resident + ",\"ledger\":\"BALANCE\",\"amount\":" + amount
                + ",\"recipient\":{\"familyMemberId\":" + recipient + "},\"method\":\"BANK_TRANSFER\","
                + "\"reason\":\"Gia đình xin rút\"}";
    }

    private String adjustment(long resident, long amount, Long original) {
        return "{\"type\":\"ADJUSTMENT\",\"residentId\":" + resident + ",\"ledger\":\"BALANCE\",\"amount\":" + amount
                + (original == null ? "" : ",\"originalTransactionId\":" + original)
                + ",\"reason\":\"Ghi nhầm người\"}";
    }

    private ResultActions newTransaction(String body) throws Exception {
        return postAs(accountant, "/api/v1/balance-transactions", body);
    }

    private ResultActions offset(long resident, long amount) throws Exception {
        return postAs(accountant, "/api/v1/residents/" + resident + "/deposit-offsets", "{\"amount\":" + amount + "}");
    }

    private long cash(long resident, String ledger, long amount) throws Exception {
        return ((Number) JsonPath.read(
                        postAs(
                                        accountant,
                                        "/api/v1/residents/" + resident + "/cash-payments",
                                        "{\"ledger\":\"" + ledger + "\",\"amount\":" + amount
                                                + ",\"payerName\":\"Lê Văn Hải\",\"receivedAt\":\"" + time.now()
                                                + "\"}")
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        "$.transactions[0].id"))
                .longValue();
    }

    private void finalized(long statement, long resident, long total) {
        as(accountant);
        tx.executeWithoutResult(s -> publisher.publishEvent(new CostStatementFinalized(
                statement, CostStatementType.REGULAR, TestUsers.nextId(), resident, total, time.now(), List.of())));
    }

    private ResultActions approve(long request) throws Exception {
        return decide(manager, request, "approve", "{\"comment\":\"Duyệt\"}");
    }

    private ResultActions reject(long request) throws Exception {
        return decide(manager, request, "reject", "{\"comment\":\"Không có căn cứ\"}");
    }

    private ResultActions cancel(long request) throws Exception {
        return decide(accountant, request, "cancel", "{\"reason\":\"Lập lại theo số dư mới\"}");
    }

    private ResultActions decide(TestUsers.TestUser who, long request, String command, String body) throws Exception {
        as(who);
        String etag = mvc.perform(get("/api/v1/approval-requests/" + request))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        return mvc.perform(post("/api/v1/approval-requests/" + request + "/" + command)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .header("If-Match", etag)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private org.springframework.test.web.servlet.RequestBuilder command(String url, String body) {
        return post(url)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static long id(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long requestOf(long transaction) {
        return jdbc.queryForObject(
                "SELECT approval_request_id FROM balance_transaction WHERE id = ?", Long.class, transaction);
    }

    private String statusOf(long transaction) {
        return jdbc.queryForObject("SELECT status FROM balance_transaction WHERE id = ?", String.class, transaction);
    }

    private long balance(long resident) {
        return jdbc.queryForObject("SELECT balance FROM resident_balance WHERE resident_id = ?", Long.class, resident);
    }
}
