package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionView;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** BF-16 (2026-11-22, chủ dự án chọn): người lập, người duyệt của giao dịch là họ tên; giao dịch tự sinh là "Hệ thống". */
class BalanceTransactionViewTest {

    private static final Map<Long, String> NAMES = Map.of(9L, "Ngô Văn Kiên", 1L, "Nguyễn Minh Quân");

    private static BalanceTransactionRow row(Long createdBy, Long approvedBy) {
        return new BalanceTransactionRow(
                5,
                7,
                Ledger.BALANCE,
                TransactionType.ADJUSTMENT,
                -100_000,
                Instant.parse("2026-11-21T16:53:00Z"),
                PaymentMethod.OTHER,
                null,
                null,
                2L,
                null,
                null,
                null,
                "Ghi thừa",
                TransactionStatus.CONFIRMED,
                1L,
                approvedBy,
                Instant.parse("2026-11-21T16:54:00Z"),
                false,
                createdBy);
    }

    @Test
    void createdByAndApprovedByAreFullNames() {
        BalanceTransactionView view = BalanceTransactionView.of(row(9L, 1L), NAMES::get);

        assertThat(view.createdBy()).isEqualTo("Ngô Văn Kiên");
        assertThat(view.approvedBy()).isEqualTo("Nguyễn Minh Quân");
    }

    @Test
    void systemTransactionsSaySystem_unknownAccountsKeepTheirCode() {
        BalanceTransactionView view = BalanceTransactionView.of(row(null, 42L), NAMES::get);

        assertThat(view.createdBy()).isEqualTo("Hệ thống");
        assertThat(view.approvedBy()).isEqualTo("Tài khoản 42");
    }
}
