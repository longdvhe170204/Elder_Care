package com.eldercare.backend.billing.balance;

import static com.eldercare.backend.billing.balance.BalanceEntries.entry;
import static com.eldercare.backend.billing.balance.BalanceEntries.withStatus;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.balance.internal.export.domain.BalanceStatement;
import com.eldercare.backend.billing.balance.internal.transaction.domain.BalanceEntry;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 017 T037 (FR-025 (a), SC-009; quickstart Q29): đầu kỳ + tổng giao dịch = cuối kỳ cho từng sổ; chỉ Đã xác nhận. */
class BalanceStatementTest {

    private static final LocalDate FROM = LocalDate.parse("2026-10-01");
    private static final LocalDate TO = LocalDate.parse("2026-10-31");

    @Test
    void FR_025_openingPlusMovementsEqualsClosing_perLedger() {
        // Q29: đầu kỳ 1.000.000, hai giao dịch, cuối kỳ 1.700.000 (số dư); cọc đi riêng
        List<BalanceEntry> history = List.of(
                entry(7, Ledger.BALANCE, TransactionType.CASH_PAYMENT, 1_000_000, "2026-09-20", "09:00"),
                entry(7, Ledger.DEPOSIT, TransactionType.DEPOSIT_RECEIPT, 3_000_000, "2026-09-25", "10:00"),
                entry(7, Ledger.BALANCE, TransactionType.BANK_TRANSFER_PAYMENT, 2_000_000, "2026-10-05", "08:30"),
                entry(7, Ledger.BALANCE, TransactionType.STATEMENT_PAYMENT, -1_300_000, "2026-10-31", "23:59"),
                // sau khoảng: không vào sao kê
                entry(7, Ledger.BALANCE, TransactionType.CASH_PAYMENT, 9_000_000, "2026-11-01", "00:00"));

        BalanceStatement statement = BalanceStatement.of(history, FROM, TO);

        assertThat(statement.openingBalance()).isEqualTo(1_000_000);
        assertThat(statement.openingDeposit()).isEqualTo(3_000_000);
        assertThat(statement.lines()).hasSize(2);
        assertThat(statement.closingBalance()).isEqualTo(1_700_000);
        assertThat(statement.closingDeposit()).isEqualTo(3_000_000);
        long movement =
                statement.lines().stream().mapToLong(BalanceEntry::amount).sum();
        assertThat(statement.openingBalance() + movement).isEqualTo(statement.closingBalance());
    }

    @Test
    void FR_025_onlyConfirmedTransactionsCount() {
        BalanceEntry confirmed = entry(7, Ledger.BALANCE, TransactionType.CASH_PAYMENT, 500_000, "2026-10-02", "09:00");
        List<BalanceEntry> history = List.of(
                confirmed,
                withStatus(
                        entry(7, Ledger.BALANCE, TransactionType.REFUND, -400_000, "2026-10-03", "09:00"),
                        TransactionStatus.PENDING_APPROVAL),
                withStatus(
                        entry(7, Ledger.BALANCE, TransactionType.REFUND, -300_000, "2026-10-04", "09:00"),
                        TransactionStatus.REJECTED),
                withStatus(
                        entry(7, Ledger.BALANCE, TransactionType.REFUND, -200_000, "2026-09-04", "09:00"),
                        TransactionStatus.CANCELLED));

        BalanceStatement statement = BalanceStatement.of(history, FROM, TO);

        assertThat(statement.openingBalance()).isZero();
        assertThat(statement.lines()).containsExactly(confirmed);
        assertThat(statement.closingBalance()).isEqualTo(500_000);
    }

    @Test
    void FR_025_rangeBoundariesFollowVietnamDays() {
        // 00:00 ngày 01/10 giờ Việt Nam vào khoảng; 23:59 ngày 30/09 thì thuộc đầu kỳ
        List<BalanceEntry> history = List.of(
                entry(7, Ledger.BALANCE, TransactionType.CASH_PAYMENT, 100, "2026-09-30", "23:59"),
                entry(7, Ledger.BALANCE, TransactionType.CASH_PAYMENT, 10, "2026-10-01", "00:00"),
                entry(7, Ledger.BALANCE, TransactionType.CASH_PAYMENT, 1, "2026-10-31", "23:59"),
                entry(7, Ledger.BALANCE, TransactionType.CASH_PAYMENT, 1000, "2026-11-01", "00:00"));

        BalanceStatement statement = BalanceStatement.of(history, FROM, TO);

        assertThat(statement.openingBalance()).isEqualTo(100);
        assertThat(statement.lines()).extracting(BalanceEntry::amount).containsExactly(10L, 1L);
        assertThat(statement.closingBalance()).isEqualTo(111);
    }
}
