package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions.Input;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementLedger;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementRules;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementRules.Basis;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.support.TableRows;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 017 T029, T030 nhánh từ chối và hàm thuần (FR-023, FR-024, FR-034; BR-M11-22; Q-224, Q-387): điều kiện lệnh Xác nhận
 * quyết toán, mục quyết toán của danh sách việc sau qua đời, nhắc sau trạng thái cuối, số liệu bảng quyết toán từ sổ.
 */
class SettlementRulesTest {

    private static final Instant T0 = Instant.parse("2026-10-20T03:00:00Z");

    private static SettlementConditions.Result result(long balance, int pending, boolean confirmed) {
        return SettlementConditions.evaluate(new Input(true, balance, 0, pending, confirmed, false));
    }

    private record ConfirmCase(Basis basis, long balance, int pending, boolean exception, List<String> codes) {}

    @Test
    void FR_034_confirmation_needsABasis_moneyOrAnException_andNoPendingTransaction() {
        TableRows.each(
                Stream.of(
                        new ConfirmCase(Basis.RESIDENCY_ENDING, 0, 0, false, List.of()),
                        new ConfirmCase(null, 0, 0, false, List.of("NOT_FINAL_STATUS")),
                        new ConfirmCase(
                                Basis.RESIDENCY_ENDING, -3_000_000, 0, false, List.of("SETTLEMENT_CONDITIONS_NOT_MET")),
                        // Q-387: ngoại lệ thay ba ý tiền, cả với danh sách việc sau qua đời
                        new ConfirmCase(Basis.POST_DEATH, -3_000_000, 0, true, List.of()),
                        // BR-M11-22: ý Chờ duyệt không miễn được
                        new ConfirmCase(Basis.RESIDENCY_ENDING, 0, 1, true, List.of("PENDING_TRANSACTIONS_REMAIN")),
                        new ConfirmCase(
                                Basis.AFTER_FINAL_STATUS,
                                500_000,
                                1,
                                false,
                                List.of("SETTLEMENT_CONDITIONS_NOT_MET", "PENDING_TRANSACTIONS_REMAIN"))),
                c -> assertThat(
                                SettlementRules.confirmationUnmet(
                                                c.basis(), result(c.balance(), c.pending(), false), c.exception())
                                        .stream()
                                        .map(SettlementRules.Unmet::code)
                                        .toList())
                        .isEqualTo(c.codes()));
        assertThat(SettlementRules.confirmationUnmet(Basis.RESIDENCY_ENDING, result(-1_500_000, 0, false), false))
                .singleElement()
                .satisfies(u -> assertThat(u.description()).contains("Số dư chưa bằng 0"));
    }

    private record ItemCase(
            boolean nonZero, long balance, boolean confirmed, boolean exception, PostDeathItemStatus to) {}

    @Test
    void FR_023_postDeathItem_notApplicableWithoutMoney_completedOnlyWhenConfirmed() {
        TableRows.each(
                Stream.of(
                        new ItemCase(false, 0, false, false, PostDeathItemStatus.NOT_APPLICABLE),
                        new ItemCase(true, 0, false, false, PostDeathItemStatus.NOT_COMPLETED),
                        new ItemCase(true, 0, true, false, PostDeathItemStatus.COMPLETED),
                        new ItemCase(true, -3_000_000, true, false, PostDeathItemStatus.NOT_COMPLETED),
                        new ItemCase(true, -3_000_000, true, true, PostDeathItemStatus.COMPLETED),
                        new ItemCase(true, -3_000_000, false, true, PostDeathItemStatus.NOT_COMPLETED)),
                c -> assertThat(SettlementRules.postDeathItem(
                                        c.nonZero(), result(c.balance(), 0, c.confirmed()), c.exception())
                                .status())
                        .isEqualTo(c.to()));
    }

    private record ReminderCase(
            boolean finalStatus,
            boolean openBasis,
            boolean afterFinal,
            boolean statement,
            boolean uncovered,
            long balance,
            boolean due) {}

    @Test
    void FR_024_afterFinalStatus_remindsUntilBalanceZeroAndConfirmedAgain() {
        TableRows.each(
                Stream.of(
                        new ReminderCase(true, false, true, true, true, 0, true),
                        new ReminderCase(true, false, true, true, false, 0, false),
                        new ReminderCase(false, false, true, true, true, 0, false),
                        // danh sách việc sau qua đời còn mở: mục quyết toán lo, không nhắc thêm
                        new ReminderCase(true, true, true, true, true, 0, false),
                        new ReminderCase(true, false, false, true, true, 0, false),
                        // chưa từng xác nhận (mục Không áp dụng): nhắc tới khi số dư về 0 (Q-224)
                        new ReminderCase(true, false, true, false, true, -200_000, true),
                        new ReminderCase(true, false, true, false, true, 0, false)),
                c -> assertThat(SettlementRules.afterFinalReminderDue(
                                c.finalStatus(),
                                c.openBasis(),
                                c.afterFinal(),
                                c.statement(),
                                c.uncovered(),
                                c.balance(),
                                0))
                        .isEqualTo(c.due()));
    }

    @Test
    void FR_034_statementFigures_comeFromTheConfirmedLedger() {
        var rows = List.of(
                row(1, Ledger.DEPOSIT, TransactionType.DEPOSIT_RECEIPT, 20_000_000, TransactionStatus.CONFIRMED),
                row(2, Ledger.BALANCE, TransactionType.CASH_PAYMENT, 4_000_000, TransactionStatus.CONFIRMED),
                row(3, Ledger.BALANCE, TransactionType.STATEMENT_PAYMENT, -9_000_000, TransactionStatus.CONFIRMED),
                row(4, Ledger.DEPOSIT, TransactionType.DEPOSIT_OFFSET, -5_000_000, TransactionStatus.CONFIRMED),
                row(5, Ledger.BALANCE, TransactionType.DEPOSIT_OFFSET, 5_000_000, TransactionStatus.CONFIRMED),
                row(6, Ledger.DEPOSIT, TransactionType.DEPOSIT_REFUND, -15_000_000, TransactionStatus.CONFIRMED),
                row(7, Ledger.BALANCE, TransactionType.REFUND, -100_000, TransactionStatus.CANCELLED),
                row(8, Ledger.BALANCE, TransactionType.REFUND, -200_000, TransactionStatus.PENDING_APPROVAL));

        SettlementLedger ledger = SettlementLedger.of(rows);

        assertThat(ledger.pending()).isEqualTo(1);
        assertThat(ledger.lastConfirmedId()).isEqualTo(6L);
        assertThat(ledger.hasNonZeroTransaction()).isTrue();
        assertThat(ledger.finalPeriodTotal()).isEqualTo(9_000_000);
        assertThat(ledger.balanceBeforeSettlement()).isEqualTo(4_000_000);
        assertThat(ledger.depositReceived()).isEqualTo(20_000_000);
        assertThat(ledger.depositOffset()).isEqualTo(5_000_000);
        assertThat(ledger.depositRefunded()).isEqualTo(15_000_000);
        assertThat(ledger.sinceFinalPeriod())
                .extracting(e -> e.amount())
                .containsExactly(-5_000_000L, 5_000_000L, -15_000_000L);
        assertThat(ledger.confirmedAfter(5L)).isTrue();
        assertThat(ledger.confirmedAfter(6L)).isFalse();
        assertThat(SettlementLedger.of(List.of()).hasNonZeroTransaction()).isFalse();
    }

    private static BalanceTransactionRow row(
            long id, Ledger ledger, TransactionType type, long amount, TransactionStatus status) {
        boolean confirmed = status == TransactionStatus.CONFIRMED;
        return new BalanceTransactionRow(
                id,
                1,
                ledger,
                type,
                amount,
                T0.plusSeconds(id),
                type == TransactionType.CASH_PAYMENT ? PaymentMethod.CASH : PaymentMethod.INTERNAL,
                null,
                type == TransactionType.STATEMENT_PAYMENT ? 99L : null,
                null,
                null,
                null,
                null,
                null,
                status,
                null,
                null,
                confirmed ? T0.plusSeconds(id) : null,
                false,
                null);
    }
}
