package com.eldercare.backend.billing.balance.internal.ledger.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceGovernanceStore;
import com.eldercare.backend.common.error.NotFoundException;

/**
 * Gắn phạm vi sổ, giao dịch theo người cao tuổi (tên logic {@code GanPhamViProvider}; module-interfaces 017 mục 2.1,
 * 002 FR-034). Phiếu báo phí, bảng quyết toán do phần chứng từ của 017 (B) đăng ký khi có bản ghi.
 */
public class BalanceScopeAssignment implements ScopeAssignmentProvider {

    private final String type;
    private final BalanceGovernanceStore store;

    public BalanceScopeAssignment(String type, BalanceGovernanceStore store) {
        this.type = type;
        this.store = store;
    }

    @Override
    public String entityType() {
        return type;
    }

    @Override
    public Target assignScope(long id) {
        long resident;
        if (BalanceTypes.LEDGER.equals(type)) {
            if (!store.hasLedger(id)) throw new NotFoundException();
            resident = id;
        } else {
            resident = store.transactionResident(id).orElseThrow(NotFoundException::new);
        }
        return new Target(type, Long.toString(id), resident, null);
    }
}
