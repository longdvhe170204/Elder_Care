package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.billing.api.dto.CostStatementType;
import com.eldercare.backend.billing.api.events.CostStatementFinalized;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData.PaymentInstructions;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeRecords;
import com.eldercare.backend.billing.balance.internal.document.domain.StatementType;
import com.eldercare.backend.billing.balance.internal.document.service.PaymentNoticeService;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.settlement.service.SettlementService;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.admission.FakeResidencyEndingReads;
import com.eldercare.backend.support.fake.billing.FakeFinalPeriodCosts;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

/**
 * 017 T040, T041 (phần B của Phase 7–10; SC-010, DBR-37, DBR-38, FR-029): hai lần phát hành phiếu và hai lần xác nhận
 * quyết toán đồng thời chỉ ra một kết quả hợp lệ; bảng quyền của phiếu, quyết toán, xuất; dòng {@code record_event} của
 * phiếu, bảng quyết toán, lần xuất không sửa, không xóa, khóa duy nhất giữ DBR-37, DBR-38.
 */
class T040_ConcurrencyAndPermissionsIT extends BalanceTestBase {

    @Autowired
    PaymentNoticeService notices;

    @Autowired
    SettlementService settlements;

    @Autowired
    FakeResidencyEndingReads endings;

    @Autowired
    com.eldercare.backend.support.fake.admission.FakeRepresentatives representatives;

    @Autowired
    FakeFinalPeriodCosts finalPeriods;

    @AfterEach
    void resetFakes() {
        endings.reset();
    }

    @Test
    void DBR_37_twoConcurrentIssues_oneNotice_andRecordsAreLocked() throws Exception {
        long a = sentForSignature(TestUsers.nextId(), null);
        long statement = TestUsers.nextId();
        PaymentNoticeData data = data(PaymentNoticeRecords.code(YearMonth.of(2026, 10), statement));
        as(accountant);

        List<Object> outcomes = concurrently(
                () -> notices.publish(BalanceQueries.target(a), a, statement, data, null),
                () -> notices.publish(BalanceQueries.target(a), a, statement, data, null));

        assertThat(outcomes.stream().filter(o -> !(o instanceof Throwable))).hasSize(1);
        assertThat(outcomes.stream().filter(o -> o instanceof Throwable))
                .singleElement()
                .satisfies(e -> assertThat(rootBusiness(e)).isEqualTo("ALREADY_ISSUED"));
        assertThat(notices.list(BalanceQueries.target(a), a)).hasSize(1);

        // DBR-37, FR-029: dòng đã xác nhận không sửa, không xóa; khóa duy nhất cùng bảng không thêm lần hai
        String key = PaymentNoticeRecords.key(statement);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM record_event WHERE unique_key = ?", Integer.class, key))
                .isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("UPDATE record_event SET content = '{}' WHERE unique_key = ?", key))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM record_event WHERE unique_key = ?", key))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system, occurred_at,"
                                + " confirmed_at, unique_key) SELECT entity_type, entity_id, module, kind, content,"
                                + " by_system, occurred_at, confirmed_at, unique_key FROM record_event WHERE unique_key = ?",
                        key))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void DBR_28_approvalRacingStatementPayment_ledgerStaysConsistent() throws Exception {
        long a = sentForSignature(TestUsers.nextId(), null);
        long recipient = TestUsers.nextId();
        representatives.representative(a, recipient);
        postAs(
                        accountant,
                        "/api/v1/residents/" + a + "/cash-payments",
                        "{\"ledger\":\"BALANCE\",\"amount\":1500000,\"payerName\":\"Lê Văn Hải\",\"receivedAt\":\""
                                + time.now() + "\"}")
                .andExpect(status().isCreated());
        long refund = ((Number) com.jayway.jsonpath.JsonPath.read(
                        postAs(
                                        accountant,
                                        "/api/v1/balance-transactions",
                                        "{\"type\":\"REFUND\",\"residentId\":" + a
                                                + ",\"ledger\":\"BALANCE\",\"amount\":1500000,\"recipient\":"
                                                + "{\"familyMemberId\":" + recipient
                                                + "},\"method\":\"BANK_TRANSFER\",\"reason\":\"Gia đình xin rút\"}")
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        "$.id"))
                .longValue();
        long request = jdbc.queryForObject(
                "SELECT approval_request_id FROM balance_transaction WHERE id = ?", Long.class, refund);
        as(manager);
        String etag = mvc.perform(get("/api/v1/approval-requests/" + request))
                .andReturn()
                .getResponse()
                .getHeader("ETag");

        // thanh toán bảng 1.000.000 (sự kiện hệ thống của 010) chạy cùng lúc với lệnh duyệt hoàn 1.500.000
        java.util.concurrent.CountDownLatch go = new java.util.concurrent.CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> payment = pool.submit(() -> {
                go.await();
                tx.executeWithoutResult(s -> publisher.publishEvent(new CostStatementFinalized(
                        TestUsers.nextId(),
                        CostStatementType.REGULAR,
                        TestUsers.nextId(),
                        a,
                        1_000_000,
                        time.now(),
                        List.of())));
                return null;
            });
            go.countDown();
            int approved = mvc.perform(post("/api/v1/approval-requests/" + request + "/approve")
                            .with(org.springframework.security.test.web.servlet.request
                                    .SecurityMockMvcRequestPostProcessors.csrf())
                            .header("If-Match", etag)
                            .header(
                                    "Idempotency-Key",
                                    java.util.UUID.randomUUID().toString())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"comment\":\"Duyệt\"}"))
                    .andReturn()
                    .getResponse()
                    .getStatus();
            payment.get();

            // ba kết quả hợp lệ: duyệt trước (hoàn đủ, rồi trừ bảng, số dư -1.000.000); trừ bảng trước nên duyệt bị
            // chặn
            // 409 và giữ Chờ duyệt; hoặc duyệt qua kiểm tra rồi áp dụng không thành (Đã hủy); số dư cuối 500.000
            String status =
                    jdbc.queryForObject("SELECT status FROM balance_transaction WHERE id = ?", String.class, refund);
            long balance =
                    jdbc.queryForObject("SELECT balance FROM resident_balance WHERE resident_id = ?", Long.class, a);
            long ledgerSum = jdbc.queryForObject(
                    "SELECT COALESCE(SUM(amount), 0) FROM balance_transaction WHERE resident_id = ?"
                            + " AND ledger = 'BALANCE' AND status = 'CONFIRMED'",
                    Long.class,
                    a);
            assertThat(balance).as("số dư khớp tổng giao dịch Đã xác nhận").isEqualTo(ledgerSum);
            if (status.equals("CONFIRMED")) {
                assertThat(approved).isEqualTo(200);
                assertThat(balance).isEqualTo(-1_000_000);
            } else if (status.equals("CANCELLED")) {
                // duyệt kịp qua kiểm tra, nhưng lúc áp dụng số dư đã bị trừ bảng: Áp dụng không thành, giao dịch Đã hủy
                assertThat(approved).isEqualTo(200);
                assertThat(balance).isEqualTo(500_000);
            } else {
                assertThat(status).isEqualTo("PENDING_APPROVAL");
                assertThat(approved).isEqualTo(409);
                assertThat(balance).isEqualTo(500_000);
            }
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM balance_transaction WHERE resident_id = ?"
                                    + " AND type = 'STATEMENT_PAYMENT'",
                            Integer.class,
                            a))
                    .isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @SafeVarargs
    private static List<Object> concurrently(Callable<Object>... tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.length);
        try {
            java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                try {
                    results.add(future.get());
                } catch (java.util.concurrent.ExecutionException e) {
                    results.add(e.getCause());
                }
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static String rootBusiness(Object failure) {
        Throwable t = (Throwable) failure;
        while (t != null && !(t instanceof BusinessException)) {
            t = t.getCause();
        }
        return t == null ? failure.toString() : ((BusinessException) t).errorCode();
    }

    private static PaymentNoticeData data(String code) {
        return new PaymentNoticeData(
                code,
                java.time.LocalDate.of(2026, 11, 3),
                "Trần Thị Kế Toán",
                "Nguyễn Thị Ánh",
                null,
                "10/2026",
                StatementType.REGULAR,
                List.of(),
                List.of(),
                1_000_000,
                2_000_000,
                1_000_000,
                new PaymentInstructions("Vietcombank", "0011004000123", "VIEN DUONG LAO BINH AN", "NCT7K2M9X4A"));
    }
}
