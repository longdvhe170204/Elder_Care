package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.dto.EndCase;
import com.eldercare.backend.admission.api.dto.PostDeathItemCode;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.admission.api.dto.ResidencyEndingView;
import com.eldercare.backend.admission.api.dto.SettlementInfoView;
import com.eldercare.backend.admission.api.events.ResidencyEndingCreated;
import com.eldercare.backend.admission.api.events.SettlementConditionChanged;
import com.eldercare.backend.admission.api.spi.PostDeathItemPort;
import com.eldercare.backend.billing.api.dto.BalanceDocumentType;
import com.eldercare.backend.billing.api.dto.CostStatementType;
import com.eldercare.backend.billing.api.dto.CostStatementView;
import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.billing.api.dto.CostTypeTotal;
import com.eldercare.backend.billing.api.dto.FinalPeriodConditionView;
import com.eldercare.backend.billing.api.events.BalanceDocumentIssued;
import com.eldercare.backend.billing.api.events.CostStatementFinalized;
import com.eldercare.backend.billing.balance.internal.status.domain.BalanceNotice;
import com.eldercare.backend.billing.balance.internal.status.service.AfterFinalStatusReminders;
import com.eldercare.backend.billing.internal.provision.domain.FinalizedCostStatement;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.admission.FakeRepresentatives;
import com.eldercare.backend.support.fake.admission.FakeResidencyEndingReads;
import com.eldercare.backend.support.fake.billing.FakeFinalPeriodCosts;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 017 T029, T030 (US7 #1 → #6, US10 #1 → #5; FR-022 → FR-024, FR-034, FR-037; BR-M11-22, DBR-38; quickstart Q16 → Q23):
 * một ca IT cho luồng chính của Phase 8 (ngân sách IT, B chọn 2026-11-08); nhánh từ chối của lệnh ở
 * {@link SettlementRulesTest}. Bean điều kiện chuyển trạng thái trả hai mã; Xác nhận quyết toán dựng bảng PDF có phiên
 * bản, một hiện hành; tải lại đúng file; bản ký một lần vào phiên bản hiện hành; mục quyết toán của danh sách việc sau qua
 * đời (Q-387); nhắc sau trạng thái cuối (Q-224).
 */
class US7_SettlementIT extends BalanceTestBase {

    @Autowired
    FakeRepresentatives representatives;

    @Autowired
    FakeResidencyEndingReads endings;

    @Autowired
    FakeFinalPeriodCosts finalPeriods;

    @Autowired
    List<StatusTransitionParticipant> participants;

    @Autowired
    List<PostDeathItemPort> postDeathItems;

    @Autowired
    FileApi files;

    @Autowired
    AfterFinalStatusReminders afterFinal;

    @AfterEach
    void resetFakes() {
        endings.reset();
        clock.reset();
    }

    @Test
    void Q16_Q23_conditionsForEnding_confirmBuildsVersionedStatement_signedCopyOnce_postDeathItem_afterFinalReminder()
            throws Exception {
        long a = sentForSignature(TestUsers.nextId(), 20_000_000L);
        long p = TestUsers.nextId();
        representatives.representative(a, p);
        relationships.name(p, "[Người đại diện kiểm thử]");
        cash(a, "DEPOSIT", 20_000_000);
        cash(a, "BALANCE", 4_000_000);
        finalPeriods.condition(a, new FinalPeriodConditionView(false, List.of("Bảng kỳ cuối chưa chốt")));
        endings.preparing(
                new ResidencyEndingView(TestUsers.nextId(), a, EndCase.CONTRACT_EXPIRED, time.today(), null),
                new SettlementInfoView(null, null, null, time.today(), EndCase.CONTRACT_EXPIRED, null));
        as(office);
        tx.executeWithoutResult(s -> publisher.publishEvent(new ResidencyEndingCreated(1, a, time.today())));
        assertThat(changed(a)).as("FR-022: báo 004 khi lập hồ sơ kết thúc").isPositive();

        // US7 #1: hai mã, ba ý tiền miễn được, ý xác nhận không
        assertThat(conditions(a))
                .extracting(Condition::code, Condition::waivable)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("END_H_SETTLEMENT", true),
                        org.assertj.core.groups.Tuple.tuple("END_H_SETTLEMENT_CONFIRMATION", false));
        assertThat(conditions(a).get(0).description()).contains("Bảng kỳ cuối chưa chốt");
        getAs(manager, "/api/v1/residents/" + a + "/settlement")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditions[0].code").value("END_H_SETTLEMENT"))
                .andExpect(jsonPath("$.conditions[0].status").value("NOT_MET"))
                .andExpect(jsonPath("$.conditions[1].waivable").value(false))
                .andExpect(jsonPath("$.versions.length()").value(0));
        getAs(office, "/api/v1/residents/" + a + "/settlement").andExpect(status().isForbidden());
        // US10 #2: chưa chốt kỳ cuối thì không xác nhận được
        confirm(a)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SETTLEMENT_CONDITIONS_NOT_MET"));

        // US7 #2, #3: kỳ cuối 9.000.000 đồng, cấn trừ 5.000.000 đồng, hoàn cọc 15.000.000 đồng được duyệt
        finalPeriods.condition(a, new FinalPeriodConditionView(true, List.of()));
        finalized(a, 9_000_000);
        postAs(accountant, "/api/v1/residents/" + a + "/deposit-offsets", "{\"amount\":5000000}")
                .andExpect(status().isCreated());
        long refund = id(postAs(accountant, "/api/v1/balance-transactions", depositRefund(a, 15_000_000, p))
                .andExpect(status().isCreated()));
        confirm(a).andExpect(status().isConflict());
        approve(requestOf(refund)).andExpect(status().isOk());
        assertThat(conditions(a)).singleElement().satisfies(c -> {
            assertThat(c.code()).isEqualTo("END_H_SETTLEMENT_CONFIRMATION");
            assertThat(c.description()).contains("Chưa xác nhận quyết toán");
        });

        // US10 #1: Kế toán xác nhận, bảng phiên bản 1; Hành chính không được
        postAs(office, "/api/v1/residents/" + a + "/settlement/confirm", "").andExpect(status().isForbidden());
        int before = changed(a);
        long v1 = id(confirm(a)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.current").value(true))
                .andExpect(jsonPath("$.hasException").value(false))
                .andExpect(jsonPath("$.debtOrRefundAmount").value(0))
                .andExpect(jsonPath("$.hasSignedCopy").value(false)));
        assertThat(conditions(a)).as("US7 #3: Đạt sau khi xác nhận").isEmpty();
        assertThat(changed(a)).isGreaterThan(before);
        assertThat(events.stream(BalanceDocumentIssued.class))
                .contains(new BalanceDocumentIssued(a, BalanceDocumentType.SETTLEMENT_STATEMENT, v1));
        assertThat(events.stream(BalanceNotice.class)
                        .flatMap(n -> n.notificationRequests().stream())
                        .filter(r -> r.eventType().equals("M11_SETTLEMENT_CONFIRMED")))
                .singleElement()
                .satisfies(r -> assertThat(r.recipientGroups())
                        .containsExactly(new RecipientGroup.FamilyWithRight(a, FamilyRight.VIEW_COSTS)));

        // FR-037: tải lại đúng file đã lưu, mỗi lần tải ghi nhật ký; nội dung khớp sổ
        byte[] first = download(v1);
        String text = DocumentReaders.pdfText(first);
        assertThat(text)
                .contains("Người đại diện: [Người đại diện kiểm thử]")
                .contains("Phiên bản 1", "Tổng bảng kỳ cuối: 9.000.000", "Số dư trước quyết toán: 4.000.000")
                .contains("Tiền cọc đã thu: 20.000.000", "đã cấn trừ: 5.000.000", "đã hoàn: 15.000.000")
                .contains("Chứng từ này không thay thế hóa đơn")
                .containsPattern("Phí lưu trú\\s*8\\.000\\.000")
                .containsPattern("Thuốc\\s*1\\.000\\.000");
        assertThat(download(v1)).isEqualTo(first);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'BALANCE_DOCUMENT_DOWNLOADED' AND entity_id = ?",
                        Integer.class,
                        Long.toString(v1)))
                .isEqualTo(2);
        as(nurse);
        mvc.perform(get("/api/v1/settlement-statements/" + v1 + "/file")).andExpect(status().isForbidden());

        // US10 #3: bản ký một lần vào phiên bản hiện hành, không đổi điều kiện
        signedCopy(v1, scan(first)).andExpect(status().isOk());
        signedCopy(v1, scan(first))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SIGNED_COPY_EXISTS"));
        assertThat(conditions(a)).isEmpty();

        // US10 #4: khoản nộp muộn làm ý xác nhận Chưa đạt; hoàn rồi xác nhận lại → phiên bản 2 hiện hành
        cash(a, "BALANCE", 300_000);
        assertThat(conditions(a))
                .extracting(Condition::description)
                .anySatisfy(d -> assertThat(d).contains("giao dịch mới sau lần xác nhận"));
        approve(requestOf(id(postAs(accountant, "/api/v1/balance-transactions", refund(a, 300_000, p))
                        .andExpect(status().isCreated()))))
                .andExpect(status().isOk());
        long v2 = id(confirm(a)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2)));
        String settlement = getAs(accountant, "/api/v1/residents/" + a + "/settlement")
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Integer> versionIds = JsonPath.read(settlement, "$.versions[*].id");
        List<Boolean> current = JsonPath.read(settlement, "$.versions[*].current");
        List<Boolean> signed = JsonPath.read(settlement, "$.versions[*].hasSignedCopy");
        assertThat(versionIds).containsExactlyInAnyOrder((int) v1, (int) v2);
        assertThat(current.get(versionIds.indexOf((int) v1))).isFalse();
        assertThat(signed.get(versionIds.indexOf((int) v1))).isTrue();
        assertThat(current.get(versionIds.indexOf((int) v2))).isTrue();
        signedCopy(v1, scan(first))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_CURRENT_VERSION"));

        // US10 #5, Q-387: người qua đời còn nợ, Quản lý viện duyệt ngoại lệ, Kế toán xác nhận → mục Hoàn thành
        long b = sentForSignature(TestUsers.nextId(), null);
        finalized(b, 3_000_000);
        finalPeriods.condition(b, new FinalPeriodConditionView(true, List.of()));
        endings.settlementInfo(b, new SettlementInfoView(null, null, null, time.today(), EndCase.DEATH, time.today()));
        assertThat(item(b)).isEqualTo(PostDeathItemStatus.NOT_COMPLETED);
        confirm(b).andExpect(status().isConflict());
        endings.approvedException(b);
        confirm(b)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasException").value(true))
                .andExpect(jsonPath("$.debtOrRefundAmount").value(-3_000_000));
        assertThat(item(b)).isEqualTo(PostDeathItemStatus.COMPLETED);
        long c = sentForSignature(TestUsers.nextId(), null);
        assertThat(item(c)).as("sổ chưa từng có giao dịch khác 0").isEqualTo(PostDeathItemStatus.NOT_APPLICABLE);

        // US7 #6, FR-024: giao dịch sau trạng thái cuối → nhắc Kế toán mỗi CFG-M02-09 tới khi xác nhận lại
        endings.reset();
        residents.changeStatus(a, ResidentStatus.RESIDENCY_ENDED, time.now());
        clock.setTo(time.now().plus(Duration.ofHours(1)));
        cash(a, "BALANCE", 100_000);
        afterFinal.remind();
        var remindedAt = jdbc.queryForObject(
                "SELECT final_status_reminded_at FROM resident_balance WHERE resident_id = ?",
                java.sql.Timestamp.class,
                a);
        assertThat(remindedAt).isNotNull();
        afterFinal.remind();
        assertThat(jdbc.queryForObject(
                        "SELECT final_status_reminded_at FROM resident_balance WHERE resident_id = ?",
                        java.sql.Timestamp.class,
                        a))
                .isEqualTo(remindedAt);
        confirm(a)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SETTLEMENT_CONDITIONS_NOT_MET"));
    }

    private List<Condition> conditions(long resident) {
        StatusTransitionParticipant mine = participants.stream()
                .filter(p -> p.getClass().getSimpleName().startsWith("Settlement"))
                .findFirst()
                .orElseThrow();
        assertThat(mine.interestedCommands()).containsExactly(StatusCommand.END_RESIDENCY);
        return tx.execute(s -> mine.checkConditions(new CommandContext(
                resident, ResidentStatus.IN_RESIDENCE, StatusCommand.END_RESIDENCY, null, time.now(), null)));
    }

    private PostDeathItemStatus item(long resident) {
        return postDeathItems.stream()
                .filter(port -> port.code() == PostDeathItemCode.SETTLEMENT)
                .findFirst()
                .orElseThrow()
                .status(resident)
                .status();
    }

    private int changed(long resident) {
        return (int) events.stream(SettlementConditionChanged.class)
                .filter(e -> e.residentId() == resident)
                .count();
    }

    private ResultActions confirm(long resident) throws Exception {
        return postAs(accountant, "/api/v1/residents/" + resident + "/settlement/confirm", "");
    }

    private ResultActions signedCopy(long statement, long file) throws Exception {
        return postAs(
                accountant, "/api/v1/settlement-statements/" + statement + "/signed-copy", "{\"fileId\":" + file + "}");
    }

    /** Bản scan có chữ ký: một tệp PDF Kế toán vừa tải lên (chưa gắn). */
    private long scan(byte[] pdf) {
        return files.upload("ban-ky.pdf", "application/pdf", pdf, accountant.accountId());
    }

    private byte[] download(long statement) throws Exception {
        as(accountant);
        return mvc.perform(get("/api/v1/settlement-statements/" + statement + "/file"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
    }

    private String refund(long resident, long amount, long recipient) {
        return "{\"type\":\"REFUND\",\"residentId\":" + resident + ",\"ledger\":\"BALANCE\",\"amount\":" + amount
                + ",\"recipient\":{\"familyMemberId\":" + recipient + "},\"method\":\"BANK_TRANSFER\","
                + "\"reason\":\"Hoàn khoản nộp muộn\"}";
    }

    private String depositRefund(long resident, long amount, long recipient) {
        return "{\"type\":\"DEPOSIT_REFUND\",\"residentId\":" + resident + ",\"ledger\":\"DEPOSIT\",\"amount\":"
                + amount + ",\"recipient\":{\"familyMemberId\":" + recipient + "},\"method\":\"CASH\","
                + "\"reason\":\"Hoàn cọc khi ra viện\"}";
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

    private void finalized(long resident, long total) {
        as(accountant);
        long statement = TestUsers.nextId();
        // 010 trả bảng Đã chốt theo mã bảng: tổng theo loại là chi tiết của bảng kỳ cuối trên bảng quyết toán (FR-034)
        costs.statement(new FinalizedCostStatement(
                1,
                time.now(),
                new CostStatementView(
                        statement,
                        "2026-10",
                        resident,
                        CostStatementType.REGULAR,
                        true,
                        java.time.LocalDate.of(2026, 10, 31),
                        "FINALIZED",
                        List.of(
                                new CostTypeTotal(CostType.RESIDENCY_FEE, total - 1_000_000),
                                new CostTypeTotal(CostType.MEDICATION, 1_000_000)),
                        total,
                        List.of())));
        tx.executeWithoutResult(s -> publisher.publishEvent(new CostStatementFinalized(
                statement, CostStatementType.REGULAR, TestUsers.nextId(), resident, total, time.now(), List.of())));
    }

    private ResultActions approve(long request) throws Exception {
        as(manager);
        String etag = mvc.perform(get("/api/v1/approval-requests/" + request))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        return mvc.perform(post("/api/v1/approval-requests/" + request + "/approve")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .header("If-Match", etag)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Duyệt\"}"));
    }

    private long requestOf(long transaction) {
        return jdbc.queryForObject(
                "SELECT approval_request_id FROM balance_transaction WHERE id = ?", Long.class, transaction);
    }

    private static long id(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
