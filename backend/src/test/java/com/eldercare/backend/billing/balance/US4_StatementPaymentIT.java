package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.billing.api.dto.CostStatementType;
import com.eldercare.backend.billing.api.events.CostStatementFinalized;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 017 T016 (US4 #1 → #5; FR-012; BR-M11-11; DBR-30; SC-002; quickstart Q7): bảng chi phí Đã chốt tạo đúng một giao dịch
 * thanh toán bảng Đã xác nhận, số tiền là tổng với dấu ngược (tổng âm làm tăng số dư, tổng 0 vẫn có giao dịch 0 đồng);
 * sự kiện lặp không tạo giao dịch thứ hai; số dư âm hiện "còn nợ".
 */
class US4_StatementPaymentIT extends BalanceTestBase {

    @Test
    void Q7_DBR_30_oneStatementPaymentPerFinalizedStatement() throws Exception {
        long a = sentForSignature(TestUsers.nextId(), null);
        long period = TestUsers.nextId();
        long october = TestUsers.nextId();
        long supplementary = TestUsers.nextId();
        long empty = TestUsers.nextId();
        finalized(october, CostStatementType.REGULAR, period, a, 9_300_000);
        finalized(october, CostStatementType.REGULAR, period, a, 9_300_000);
        finalized(supplementary, CostStatementType.SUPPLEMENTARY, period, a, -200_000);
        finalized(empty, CostStatementType.REGULAR, TestUsers.nextId(), a, 0);

        List<Long> amounts = jdbc.queryForList(
                "SELECT amount FROM balance_transaction WHERE resident_id = ? AND type = 'STATEMENT_PAYMENT'"
                        + " AND status = 'CONFIRMED' AND method = 'INTERNAL' ORDER BY id",
                Long.class,
                a);
        assertThat(amounts).containsExactly(-9_300_000L, 200_000L, 0L);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM balance_transaction WHERE cost_statement_id = ?", Integer.class, october))
                .isOne();
        getAs(accountant, "/api/v1/residents/" + a + "/balance")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(-9_100_000))
                .andExpect(jsonPath("$.debt").value(9_100_000));
        getAs(accountant, "/api/v1/residents/" + a + "/balance-transactions?ledger=BALANCE")
                .andExpect(jsonPath("$[0].type").value("STATEMENT_PAYMENT"))
                .andExpect(jsonPath("$[0].costStatementId").value(empty))
                .andExpect(jsonPath("$[0].createdBy").value("SYSTEM"));
    }

    private void finalized(long statement, CostStatementType type, long period, long resident, long total) {
        as(accountant);
        tx.executeWithoutResult(s -> publisher.publishEvent(
                new CostStatementFinalized(statement, type, period, resident, total, time.now(), List.of())));
    }
}
