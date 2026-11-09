package com.eldercare.backend.billing.balance.internal.ledger.service;

import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.ledger.domain.ConfirmedEntry;
import com.eldercare.backend.billing.balance.internal.ledger.domain.LedgerState;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceTransactionStore;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi giao dịch Đã xác nhận và cập nhật sổ cùng transaction dưới khóa dòng sổ (FR-002, DBR-28, research E-03). Chỉ lệnh
 * của Kế toán, lệnh duyệt và lệnh chốt bảng của 010 gọi; job không gọi ({@code M11ArchTest}).
 */
@Service
public class LedgerPosting {

    private final LedgerStore ledgers;
    private final BalanceTransactionStore transactions;
    private final BusinessTime time;

    public LedgerPosting(LedgerStore ledgers, BalanceTransactionStore transactions, BusinessTime time) {
        this.ledgers = ledgers;
        this.transactions = transactions;
        this.time = time;
    }

    /** 409 {@code NO_LEDGER} khi hồ sơ chưa có sổ (hợp đồng đầu tiên chưa Chờ ký). */
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerState lock(long residentId) {
        return ledgers.lock(residentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT, "NO_LEDGER", "Người cao tuổi chưa có sổ số dư (hợp đồng chưa gửi ký)"));
    }

    /** Gọi sau {@link #lock}. */
    @Transactional(propagation = Propagation.MANDATORY)
    public BalanceTransactionRow postConfirmed(ConfirmedEntry entry) {
        long id = transactions.insertConfirmed(entry, time.now());
        ledgers.add(entry.residentId(), entry.ledger(), entry.amount());
        return transactions.find(id).orElseThrow();
    }
}
