package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceDetails;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionView;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.billing.balance.web.response.PortalBalanceResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** BF-16 (chủ dự án chốt 2026-11-22): cổng người thân không thấy ghi chú nội bộ của giao dịch; các trường khác giữ. */
class PortalBalanceResponseTest {

    @Test
    void portalHidesInternalNote_keepsReasonAndAmounts() {
        BalanceTransactionView cash = new BalanceTransactionView(
                2,
                Ledger.BALANCE,
                TransactionType.CASH_PAYMENT,
                3_000_000,
                Instant.parse("2026-11-21T16:45:00Z"),
                PaymentMethod.CASH,
                null,
                null,
                null,
                null,
                "Ngô Văn Kiên",
                null,
                "Ghi thừa",
                "PT 0123, ghi chú nội bộ",
                TransactionStatus.CONFIRMED,
                false,
                null);
        BalanceDetails balance = new BalanceDetails(
                9004, "NCTPKFCFCN", 3_000_000, 0, null, null, null, null, null, null, null, null, null);

        PortalBalanceResponse response = PortalBalanceResponse.of(new BalanceQueries.Portal(balance, List.of(cash)));

        BalanceTransactionView shown = response.transactions().getFirst();
        assertThat(shown.note()).isNull();
        assertThat(shown.reason()).isEqualTo("Ghi thừa");
        assertThat(shown.amount()).isEqualTo(3_000_000);
        assertThat(shown.type()).isEqualTo(TransactionType.CASH_PAYMENT);
    }
}
