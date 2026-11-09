package com.eldercare.backend.billing.balance.internal.ledger.service;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.billing.api.events.CostStatementFinalized;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.ledger.domain.ConfirmedEntry;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceTransactionStore;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bảng chi phí Đã chốt tự trừ số dư (017 T017; FR-012, BR-M11-11, DBR-30; research E-06): tác động kèm lệnh chốt của
 * 010, nghe trong transaction của lệnh đó. Một giao dịch "Thanh toán bảng chi phí" Đã xác nhận mỗi bảng, số tiền là tổng
 * với dấu ngược (tổng 0 vẫn tạo giao dịch 0 đồng); sự kiện lặp bỏ qua ({@code UNIQUE(cost_statement_id)} giữ khi chạy
 * đồng thời). Không tính lại tình trạng: số dư và tổng chưa chốt cùng giảm đúng tổng bảng nên (số dư − tổng chưa chốt)
 * không đổi; tình trạng chỉ đổi ở lần tính hằng ngày và khi ghi tiền nộp (bảng chuyển tình trạng số dư).
 */
@Service
class StatementPayments {

    static final AuditAction PAID = new AuditAction("BALANCE_STATEMENT_PAID", false);
    private static final Logger LOG = LoggerFactory.getLogger(StatementPayments.class);

    private final LedgerPosting posting;
    private final LedgerStore ledgers;
    private final BalanceTransactionStore transactions;
    private final AuditLogApi audit;

    StatementPayments(
            LedgerPosting posting, LedgerStore ledgers, BalanceTransactionStore transactions, AuditLogApi audit) {
        this.posting = posting;
        this.ledgers = ledgers;
        this.transactions = transactions;
        this.audit = audit;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(CostStatementFinalized event) {
        if (ledgers.lock(event.residentId()).isEmpty()) {
            // Mọi người có khoản chi phí đều có hợp đồng đã gửi ký nên có sổ; không chặn lệnh chốt của 010.
            LOG.warn("Bảng chi phí {} của người cao tuổi {} chưa có sổ số dư", event.statementId(), event.residentId());
            return;
        }
        if (transactions.statementPaid(event.statementId())) return;
        var row = posting.postConfirmed(new ConfirmedEntry(
                event.residentId(),
                Ledger.BALANCE,
                TransactionType.STATEMENT_PAYMENT,
                -event.total(),
                event.finalizedAt(),
                PaymentMethod.INTERNAL,
                null,
                event.statementId(),
                null,
                null,
                false,
                null));
        audit.write(AuditLogEntry.builder()
                .action(PAID)
                .entity(BalanceTypes.TRANSACTION, row.id())
                .resident(event.residentId())
                .after(Map.of("costStatementId", event.statementId(), "amount", row.amount()))
                .basis(new Basis("BR-M11-11", null, event.statementId(), null))
                .build());
    }
}
