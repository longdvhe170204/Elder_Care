package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.billing.api.dto.CostStatementType;
import com.eldercare.backend.billing.api.events.CostStatementFinalized;
import com.eldercare.backend.billing.balance.internal.ledger.domain.Payments;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.ledger.service.PaymentService;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.support.TestUsers;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

/**
 * 017 T040, T041, T016 phần A (DBR-28, DBR-30, DBR-17 ở tầng cơ sở dữ liệu; SC-001, SC-002; quickstart Q7): sau ba tháng
 * nộp tiền và chốt bảng của 50 hồ sơ, số dư, số dư cọc bằng tổng giao dịch Đã xác nhận; mỗi bảng đúng một giao dịch
 * thanh toán (sự kiện lặp, tổng âm, tổng 0); hai lần nộp đồng thời đều vào sổ; giao dịch Đã xác nhận không sửa, xóa
 * được trừ dấu "đã điều chỉnh"; đối chiếu hằng đêm tìm ra sổ lệch (E-03).
 */
class DBR_28_BalanceInvariantIT extends BalanceTestBase {

    @Autowired
    PaymentService payments;

    @Autowired
    LedgerStore ledgers;

    @Test
    void DBR_28_DBR_30_ledgersEqualConfirmedTransactions_afterThreeMonths_andUnderConcurrency() throws Exception {
        Random random = new Random(28);
        List<Long> residents = new ArrayList<>();
        List<Long> statements = new ArrayList<>();
        for (int i = 0; i < 50; i++) residents.add(sentForSignature(TestUsers.nextId(), null));
        as(accountant);
        for (int month = 0; month < 3; month++) {
            for (long resident : residents) {
                payments.recordCash(
                        BalanceQueries.target(resident),
                        resident,
                        new Payments.Cash(
                                random.nextBoolean() ? Ledger.BALANCE : Ledger.DEPOSIT,
                                1_000_000L + random.nextInt(9) * 500_000L,
                                "Lê Văn Hải",
                                time.now(),
                                null));
                long statement = TestUsers.nextId();
                statements.add(statement);
                long total = month == 2 && resident % 5 == 0 ? 0 : 3_000_000L + random.nextInt(7) * 300_000L;
                finalized(statement, CostStatementType.REGULAR, resident, total);
                if (month == 0) finalized(statement, CostStatementType.REGULAR, resident, total);
                if (month == 1 && resident % 7 == 0) {
                    long supplementary = TestUsers.nextId();
                    statements.add(supplementary);
                    finalized(supplementary, CostStatementType.SUPPLEMENTARY, resident, -200_000);
                }
            }
        }

        String in = residents.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElseThrow();
        List<Long> mismatched = jdbc.queryForList(
                "SELECT b.resident_id FROM resident_balance b WHERE b.resident_id IN (" + in + ") AND ("
                        + " b.balance <> (SELECT COALESCE(SUM(t.amount), 0) FROM balance_transaction t"
                        + "   WHERE t.resident_id = b.resident_id AND t.ledger = 'BALANCE' AND t.status = 'CONFIRMED')"
                        + " OR b.deposit_balance <> (SELECT COALESCE(SUM(t.amount), 0) FROM balance_transaction t"
                        + "   WHERE t.resident_id = b.resident_id AND t.ledger = 'DEPOSIT' AND t.status = 'CONFIRMED'))",
                Long.class);
        assertThat(mismatched)
                .as("số dư = tổng giao dịch Đã xác nhận (FR-002, DBR-28)")
                .isEmpty();
        assertThat(ledgers.mismatches()).doesNotContainAnyElementsOf(residents);
        String statementIn = statements.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElseThrow();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM balance_transaction WHERE type = 'STATEMENT_PAYMENT' AND cost_statement_id IN ("
                                + statementIn + ")",
                        Integer.class))
                .as("một giao dịch mỗi bảng, kể cả tổng 0 và sự kiện lặp (DBR-30)")
                .isEqualTo(statements.size());

        long a = residents.getFirst();
        long balance = jdbc.queryForObject("SELECT balance FROM resident_balance WHERE resident_id = ?", Long.class, a);
        getAs(accountant, "/api/v1/residents/" + a + "/balance-transactions?ledger=BALANCE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("STATEMENT_PAYMENT"))
                .andExpect(jsonPath("$[0].createdBy").value("SYSTEM"));
        if (balance < 0) {
            getAs(accountant, "/api/v1/residents/" + a + "/balance")
                    .andExpect(jsonPath("$.debt").value(-balance));
        }

        // Hai lần nộp đồng thời cho cùng hồ sơ: khóa dòng sổ giữ đúng tổng (E-03)
        as(accountant);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> runs = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            runs.add(pool.submit(() -> {
                start.await();
                return payments.recordCash(
                        BalanceQueries.target(a),
                        a,
                        new Payments.Cash(Ledger.BALANCE, 1_000_000L, "Lê Văn Hải", time.now(), null));
            }));
        }
        start.countDown();
        for (Future<?> run : runs) run.get();
        pool.shutdown();
        assertThat(jdbc.queryForObject("SELECT balance FROM resident_balance WHERE resident_id = ?", Long.class, a))
                .isEqualTo(balance + 2_000_000L);

        // DBR-17 ở tầng cơ sở dữ liệu: giao dịch Đã xác nhận chỉ được đặt dấu "đã điều chỉnh" một lần
        long confirmed = jdbc.queryForObject(
                "SELECT MIN(id) FROM balance_transaction WHERE resident_id = ? AND status = 'CONFIRMED'",
                Long.class,
                a);
        assertThatThrownBy(
                        () -> jdbc.update("UPDATE balance_transaction SET amount = amount + 1 WHERE id = ?", confirmed))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM balance_transaction WHERE id = ?", confirmed))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.update("UPDATE balance_transaction SET adjusted = TRUE WHERE id = ?", confirmed))
                .isOne();
        assertThatThrownBy(() -> jdbc.update("UPDATE balance_transaction SET adjusted = FALSE WHERE id = ?", confirmed))
                .isInstanceOf(DataAccessException.class);

        // Đối chiếu hằng đêm tìm ra sổ lệch, không tự sửa (E-03)
        jdbc.update("UPDATE resident_balance SET balance = balance + 1 WHERE resident_id = ?", a);
        try {
            assertThat(ledgers.mismatches()).contains(a);
        } finally {
            jdbc.update("UPDATE resident_balance SET balance = balance - 1 WHERE resident_id = ?", a);
        }
    }

    private void finalized(long statement, CostStatementType type, long resident, long total) {
        as(accountant);
        tx.executeWithoutResult(s -> publisher.publishEvent(new CostStatementFinalized(
                statement, type, TestUsers.nextId(), resident, total, time.now(), List.of())));
    }
}
