package com.eldercare.backend.billing.balance.internal.ledger.service;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.dto.ContractView;
import com.eldercare.backend.admission.api.dto.DepositAmountView;
import com.eldercare.backend.admission.api.dto.DepositStatus;
import com.eldercare.backend.admission.api.dto.DepositStatusView;
import com.eldercare.backend.admission.api.events.DepositAmountChanged;
import com.eldercare.backend.admission.api.events.DepositStatusChanged;
import com.eldercare.backend.admission.api.spi.DepositStatusPort;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.ledger.domain.DepositStatusRule;
import com.eldercare.backend.billing.balance.internal.ledger.domain.LedgerState;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceTransactionStore;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trạng thái đặt cọc dẫn xuất (tên logic {@code TrangThaiDatCocPort}; FR-006, research E-05): tính lúc gọi từ khoản cọc
 * của hợp đồng và số dư cọc, kèm giao dịch cọc gần nhất và Kế toán lập. Mỗi lần kết quả của hợp đồng hiện hành (Chờ ký
 * hoặc Hiệu lực) đổi, phát {@link DepositStatusChanged} cho 004 trong transaction làm đổi.
 */
@Service
public class DepositStatuses implements DepositStatusPort {

    private static final Set<ContractStatus> CURRENT = Set.of(ContractStatus.PENDING_SIGNATURE, ContractStatus.ACTIVE);

    private final ContractReadApi contracts;
    private final LedgerStore ledgers;
    private final BalanceTransactionStore transactions;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public DepositStatuses(
            ContractReadApi contracts,
            LedgerStore ledgers,
            BalanceTransactionStore transactions,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.contracts = contracts;
        this.ledgers = ledgers;
        this.transactions = transactions;
        this.events = events;
        this.time = time;
    }

    @Override
    @Transactional(readOnly = true)
    public DepositStatusView status(long contractId) {
        ContractView contract = contracts.contract(contractId).orElse(null);
        if (contract == null) return new DepositStatusView(DepositStatus.NOT_SATISFIED, null, null, null);
        DepositStatus status = compute(contractId, depositBalance(contract.residentId()));
        Optional<BalanceTransactionRow> last = transactions.lastConfirmed(contract.residentId(), Ledger.DEPOSIT);
        return new DepositStatusView(
                status,
                last.map(BalanceTransactionRow::confirmedAt).orElse(null),
                last.map(BalanceTransactionRow::id).orElse(null),
                last.map(BalanceTransactionRow::createdBy).orElse(null));
    }

    /** Hợp đồng Chờ ký hoặc Hiệu lực mới nhất của người cao tuổi. */
    public Optional<ContractView> currentContract(long residentId) {
        return contracts.ofResident(residentId).stream()
                .filter(c -> CURRENT.contains(c.status()))
                .max(Comparator.comparingLong(ContractView::id));
    }

    /** Trạng thái đặt cọc của hợp đồng hiện hành; rỗng khi không có hợp đồng Chờ ký, Hiệu lực. */
    public Optional<DepositStatus> current(long residentId, long depositBalance) {
        return currentContract(residentId).map(c -> compute(c.id(), depositBalance));
    }

    /** Ghi trạng thái ban đầu khi mở sổ, không báo (chưa có lần đổi nào). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void initialize(long residentId, long contractId) {
        ledgers.saveDepositStatus(residentId, contractId, compute(contractId, 0));
    }

    /**
     * Tính lại sau khi sổ cọc hoặc khoản cọc đổi (FR-006); khác lần đã báo thì lưu và phát {@link DepositStatusChanged}
     * kèm giao dịch làm đổi, Kế toán lập (rỗng khi do khoản cọc đổi).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recheck(long residentId, Long transactionId, Long accountantId) {
        LedgerState ledger = ledgers.find(residentId).orElse(null);
        ContractView contract = currentContract(residentId).orElse(null);
        if (ledger == null || contract == null) return;
        DepositStatus status = compute(contract.id(), ledger.depositBalance());
        if (status == ledger.depositStatus() && Long.valueOf(contract.id()).equals(ledger.depositStatusContractId())) {
            return;
        }
        ledgers.saveDepositStatus(residentId, contract.id(), status);
        events.publishEvent(
                new DepositStatusChanged(contract.id(), residentId, status, transactionId, accountantId, time.now()));
    }

    /** Khoản cọc của hợp đồng đổi (004, phụ lục đổi khoản cọc): nghe trong transaction của lệnh 004. */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(DepositAmountChanged event) {
        contracts.contract(event.contractId()).ifPresent(c -> recheck(c.residentId(), null, null));
    }

    private DepositStatus compute(long contractId, long depositBalance) {
        Optional<DepositAmountView> amount = contracts.depositAmount(contractId);
        OptionalLong required = amount.isEmpty() || amount.get().notRequired()
                ? (amount.isEmpty() ? OptionalLong.of(Long.MAX_VALUE) : OptionalLong.empty())
                : OptionalLong.of(amount.get().amount().longValueExact());
        return DepositStatusRule.of(required, depositBalance);
    }

    private long depositBalance(long residentId) {
        return ledgers.find(residentId).map(LedgerState::depositBalance).orElse(0L);
    }
}
