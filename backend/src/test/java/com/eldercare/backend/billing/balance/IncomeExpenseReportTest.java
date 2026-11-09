package com.eldercare.backend.billing.balance;

import static com.eldercare.backend.billing.balance.BalanceEntries.entry;
import static com.eldercare.backend.billing.balance.BalanceEntries.withStatus;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.balance.internal.export.domain.IncomeExpenseReport;
import com.eldercare.backend.billing.balance.internal.export.domain.IncomeExpenseReport.Debtor;
import com.eldercare.backend.billing.balance.internal.transaction.domain.BalanceEntry;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 017 T037 (FR-025 (b), Q-386; quickstart Q30): báo cáo thu chi thu gọn, ba tổng và danh sách còn nợ. */
class IncomeExpenseReportTest {

    private static final LocalDate FROM = LocalDate.parse("2026-10-01");
    private static final LocalDate TO = LocalDate.parse("2026-10-31");

    @Test
    void FR_025_threeTotalsAndDebtors() {
        // Q30: nộp 55.000.000 (số dư và cọc), trừ 48.000.000, hoàn 2.000.000, B (id 8) nợ 1.500.000
        List<BalanceEntry> entries = List.of(
                entry(7, Ledger.BALANCE, TransactionType.CASH_PAYMENT, 30_000_000, "2026-10-02", "09:00"),
                entry(7, Ledger.BALANCE, TransactionType.BANK_TRANSFER_PAYMENT, 20_000_000, "2026-10-03", "09:00"),
                entry(7, Ledger.DEPOSIT, TransactionType.DEPOSIT_RECEIPT, 5_000_000, "2026-10-04", "09:00"),
                entry(7, Ledger.BALANCE, TransactionType.STATEMENT_PAYMENT, -30_000_000, "2026-10-31", "10:00"),
                entry(8, Ledger.BALANCE, TransactionType.STATEMENT_PAYMENT, -18_000_000, "2026-10-31", "10:05"),
                entry(7, Ledger.BALANCE, TransactionType.REFUND, -1_500_000, "2026-10-20", "09:00"),
                entry(7, Ledger.DEPOSIT, TransactionType.DEPOSIT_REFUND, -500_000, "2026-10-21", "09:00"));

        IncomeExpenseReport report =
                IncomeExpenseReport.of(entries, Map.of(7L, 18_500_000L, 8L, -1_500_000L), FROM, TO);

        assertThat(report.totalPaid()).isEqualTo(55_000_000);
        assertThat(report.totalDeducted()).isEqualTo(48_000_000);
        assertThat(report.totalRefunded()).isEqualTo(2_000_000);
        assertThat(report.debtors()).containsExactly(new Debtor(8, 1_500_000));
    }

    @Test
    void FR_025_pendingRejectedAndCancelledTransactionsAreLeftOut() {
        BalanceEntry pending = withStatus(
                entry(7, Ledger.BALANCE, TransactionType.REFUND, -1_000, "2026-10-05", "09:00"),
                TransactionStatus.PENDING_APPROVAL);
        BalanceEntry rejected = withStatus(
                entry(7, Ledger.BALANCE, TransactionType.REFUND, -2_000, "2026-10-06", "09:00"),
                TransactionStatus.REJECTED);
        BalanceEntry cancelled = withStatus(
                entry(7, Ledger.BALANCE, TransactionType.CASH_PAYMENT, 4_000, "2026-10-07", "09:00"),
                TransactionStatus.CANCELLED);

        IncomeExpenseReport report = IncomeExpenseReport.of(List.of(pending, rejected, cancelled), Map.of(), FROM, TO);

        assertThat(report.totalPaid()).isZero();
        assertThat(report.totalRefunded()).isZero();
        assertThat(report.totalDeducted()).isZero();
    }

    @Test
    void Q_386_adjustmentsAndOffsetsAreNotTotalledAndOutOfRangeIsIgnored() {
        List<BalanceEntry> entries = List.of(
                entry(7, Ledger.BALANCE, TransactionType.ADJUSTMENT, 777, "2026-10-08", "09:00"),
                entry(7, Ledger.DEPOSIT, TransactionType.DEPOSIT_OFFSET, -3_000, "2026-10-09", "09:00"),
                entry(7, Ledger.BALANCE, TransactionType.DEPOSIT_OFFSET, 3_000, "2026-10-09", "09:00"),
                entry(7, Ledger.BALANCE, TransactionType.CASH_PAYMENT, 9_999, "2026-09-30", "23:59"),
                entry(7, Ledger.BALANCE, TransactionType.CASH_PAYMENT, 8_888, "2026-11-01", "00:00"));

        IncomeExpenseReport report = IncomeExpenseReport.of(entries, Map.of(), FROM, TO);

        assertThat(report.totalPaid()).isZero();
        assertThat(report.totalDeducted()).isZero();
        assertThat(report.totalRefunded()).isZero();
    }

    @Test
    void FR_025_aNegativeStatementReducesTheDeductedTotal_andDebtorsAreOrderedByDebt() {
        // bảng tổng âm (hoàn chi phí) ghi giao dịch thanh toán dương, làm giảm tổng đã trừ
        List<BalanceEntry> entries = List.of(
                entry(7, Ledger.BALANCE, TransactionType.STATEMENT_PAYMENT, -10_000, "2026-10-10", "09:00"),
                entry(8, Ledger.BALANCE, TransactionType.STATEMENT_PAYMENT, 2_500, "2026-10-11", "09:00"));

        IncomeExpenseReport report =
                IncomeExpenseReport.of(entries, Map.of(9L, -300L, 10L, 0L, 11L, 50L, 12L, -900L, 13L, -300L), FROM, TO);

        assertThat(report.totalDeducted()).isEqualTo(7_500);
        assertThat(report.debtors()).containsExactly(new Debtor(12, 900), new Debtor(9, 300), new Debtor(13, 300));
    }
}
