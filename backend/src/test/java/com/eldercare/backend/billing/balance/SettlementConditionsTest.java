package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions.Code;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions.Condition;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions.Input;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions.Reason;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions.Result;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions.Status;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 017 T028 (BR-M11-22, FR-022; research E-08): hai mã điều kiện kết thúc lưu trú. Ba ý về tiền miễn được bằng ngoại lệ,
 * ý xác nhận quyết toán thì không.
 */
class SettlementConditionsTest {

    private static Input settled() {
        return new Input(true, 0, 0, 0, true, false);
    }

    @Test
    void BR_M11_22_allMoneyAndConfirmationMet() {
        Result result = SettlementConditions.evaluate(settled());

        assertThat(result.settlement()).isEqualTo(new Condition(Code.END_H_SETTLEMENT, Status.MET, true, List.of()));
        assertThat(result.confirmation())
                .isEqualTo(new Condition(Code.END_H_SETTLEMENT_CONFIRMATION, Status.MET, false, List.of()));
        assertThat(result.satisfied(false)).isTrue();
    }

    @Test
    void BR_M11_22_eachMoneyLimbFailsWithItsOwnReason() {
        Result noFinalPaid = SettlementConditions.evaluate(new Input(false, 0, 0, 0, true, false));
        Result balanceLeft = SettlementConditions.evaluate(new Input(true, 1_500_000, 0, 0, true, false));
        Result inDebt = SettlementConditions.evaluate(new Input(true, -1_500_000, 0, 0, true, false));
        Result depositLeft = SettlementConditions.evaluate(new Input(true, 0, 3_000_000, 0, true, false));
        Result all = SettlementConditions.evaluate(new Input(false, 10, 20, 0, true, false));

        assertThat(noFinalPaid.settlement().status()).isEqualTo(Status.NOT_MET);
        assertThat(noFinalPaid.settlement().reasons()).containsExactly(Reason.FINAL_PERIOD_NOT_PAID);
        assertThat(balanceLeft.settlement().reasons()).containsExactly(Reason.BALANCE_NOT_ZERO);
        assertThat(inDebt.settlement().reasons()).containsExactly(Reason.BALANCE_NOT_ZERO);
        assertThat(depositLeft.settlement().reasons()).containsExactly(Reason.DEPOSIT_NOT_ZERO);
        assertThat(all.settlement().reasons())
                .containsExactly(Reason.FINAL_PERIOD_NOT_PAID, Reason.BALANCE_NOT_ZERO, Reason.DEPOSIT_NOT_ZERO);
        // ba ý tiền là điều kiện miễn được (004 áp ngoại lệ)
        assertThat(all.settlement().waivable()).isTrue();
        // ý xác nhận không liên quan tới ba ý tiền
        assertThat(all.confirmation().status()).isEqualTo(Status.MET);
    }

    @Test
    void BR_M11_22_exceptionWaivesOnlyTheMoneyLimbs() {
        Result moneyOpen = SettlementConditions.evaluate(new Input(false, 10, 20, 0, true, false));

        assertThat(moneyOpen.satisfied(false)).isFalse();
        assertThat(moneyOpen.satisfied(true)).isTrue();
    }

    @Test
    void BR_M11_22_pendingTransactionsKeepConfirmationUnmetEvenWithException() {
        Result result = SettlementConditions.evaluate(new Input(true, 0, 0, 2, true, false));

        assertThat(result.settlement().status()).isEqualTo(Status.MET);
        assertThat(result.confirmation().status()).isEqualTo(Status.NOT_MET);
        assertThat(result.confirmation().reasons()).containsExactly(Reason.PENDING_TRANSACTIONS);
        assertThat(result.confirmation().waivable()).isFalse();
        assertThat(result.satisfied(true)).isFalse();
    }

    @Test
    void BR_M11_22_noCurrentConfirmationOrTransactionAfterItLeavesConfirmationUnmet() {
        Result none = SettlementConditions.evaluate(new Input(true, 0, 0, 0, false, false));
        Result after = SettlementConditions.evaluate(new Input(true, 0, 0, 0, true, true));
        Result both = SettlementConditions.evaluate(new Input(true, 0, 0, 1, false, false));

        assertThat(none.confirmation().reasons()).containsExactly(Reason.NO_CURRENT_CONFIRMATION);
        assertThat(after.confirmation().reasons()).containsExactly(Reason.TRANSACTION_AFTER_CONFIRMATION);
        assertThat(both.confirmation().reasons())
                .containsExactly(Reason.PENDING_TRANSACTIONS, Reason.NO_CURRENT_CONFIRMATION);
        assertThat(none.satisfied(true)).isFalse();
        assertThat(after.satisfied(true)).isFalse();
    }
}
