package com.eldercare.backend.billing.balance.internal.settlement.service;

import com.eldercare.backend.admission.api.dto.PostDeathItemCode;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatusView;
import com.eldercare.backend.admission.api.spi.PostDeathItemPort;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceTransactionStore;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementLedger;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementRules;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản cài {@code MucViecSauQuaDoiPort} của 017 ({@code ma() = QUYET_TOAN}; T031, FR-023, Q-387; research E-08): Không áp
 * dụng khi hai sổ chưa từng có giao dịch khác 0 đồng; Hoàn thành khi đã xác nhận quyết toán và ba ý tiền đạt hoặc có
 * ngoại lệ đã duyệt; tính lúc hỏi.
 */
@Component
class SettlementPostDeathItem implements PostDeathItemPort {

    private final SettlementService settlement;
    private final BalanceTransactionStore transactions;

    SettlementPostDeathItem(SettlementService settlement, BalanceTransactionStore transactions) {
        this.settlement = settlement;
        this.transactions = transactions;
    }

    @Override
    public PostDeathItemCode code() {
        return PostDeathItemCode.SETTLEMENT;
    }

    @Override
    @Transactional(readOnly = true)
    public PostDeathItemStatusView status(long residentId) {
        boolean money = SettlementLedger.of(
                        transactions.list(residentId, null, null, null, TransactionStatus.CONFIRMED))
                .hasNonZeroTransaction();
        if (!money) {
            return new PostDeathItemStatusView(
                    PostDeathItemStatus.NOT_APPLICABLE, "Sổ số dư, sổ tiền cọc chưa từng có giao dịch khác 0 đồng");
        }
        var evaluation = settlement.evaluate(residentId);
        return SettlementRules.postDeathItem(true, evaluation.result(), evaluation.exceptionApproved());
    }
}
