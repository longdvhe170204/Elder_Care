package com.eldercare.backend.billing.balance.internal.ledger.service;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.events.ContractStatusChanged;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.ledger.domain.PaymentCode;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.time.BusinessTime;
import java.security.SecureRandom;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mở sổ khi hợp đồng đầu tiên của hồ sơ chuyển Chờ ký (FR-001, research E-04): tác động kèm lệnh gửi ký của 004, nghe
 * trong transaction của lệnh đó. Một sổ, một mã mỗi hồ sơ: sự kiện lặp, trả về nháp rồi gửi ký lại, gia hạn không mở thêm.
 * Hồ sơ mới của người quay lại là mã người cao tuổi mới nên có sổ mới.
 */
@Service
class LedgerOpening {

    static final AuditAction OPENED = new AuditAction("BALANCE_LEDGER_OPENED", false);

    private final LedgerStore ledgers;
    private final DepositStatuses deposits;
    private final TechnicalLocks locks;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final SecureRandom random = new SecureRandom();

    LedgerOpening(
            LedgerStore ledgers, DepositStatuses deposits, TechnicalLocks locks, AuditLogApi audit, BusinessTime time) {
        this.ledgers = ledgers;
        this.deposits = deposits;
        this.locks = locks;
        this.audit = audit;
        this.time = time;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(ContractStatusChanged event) {
        if (event.to() != ContractStatus.PENDING_SIGNATURE) return;
        long resident = event.residentId();
        locks.lock(BalanceTypes.LEDGER + ":" + resident);
        if (ledgers.find(resident).isPresent()) return;
        String code = PaymentCode.generate(random);
        while (ledgers.paymentCodeTaken(code)) code = PaymentCode.generate(random);
        ledgers.open(resident, event.contractId(), code, time.now());
        deposits.initialize(resident, event.contractId());
        audit.write(AuditLogEntry.builder()
                .action(OPENED)
                .entity(BalanceTypes.LEDGER, resident)
                .resident(resident)
                .after(Map.of("paymentCode", code, "balance", 0, "depositBalance", 0))
                .basis(new Basis("FR-001", null, event.contractId(), null))
                .build());
    }
}
