package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions.Input;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions.Reason;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions.Result;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementConditions.Status;
import org.junit.jupiter.api.Test;

/**
 * 017 T028 (BR-M11-22, FR-022; research E-08): hai mã điều kiện kết thúc lưu trú. Ba ý về tiền miễn được bằng ngoại lệ,
 * ý xác nhận quyết toán thì không.
 */
class SettlementConditionsTest {

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
