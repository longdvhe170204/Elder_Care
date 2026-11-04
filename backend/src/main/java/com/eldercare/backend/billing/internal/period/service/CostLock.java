package com.eldercare.backend.billing.internal.period.service;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.billing.internal.period.domain.BillingResidentState;
import com.eldercare.backend.billing.internal.period.persistence.CostRowLocks;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** T007/P-03: tuần tự hóa theo người trước khi khóa bảng; giữ transaction của lệnh gọi. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class CostLock {
    private final TechnicalLocks technicalLocks;
    private final ResidentModuleStateApi states;
    private final CostRowLocks rows;

    public CostLock(TechnicalLocks technicalLocks, ResidentModuleStateApi states, CostRowLocks rows) {
        this.technicalLocks = technicalLocks;
        this.states = states;
        this.rows = rows;
    }

    public BillingResidentState resident(long residentId) {
        // Khóa kỹ thuật bảo vệ cả trường hợp hai lệnh đầu cùng thấy chưa có dòng trạng thái.
        technicalLocks.lock("M11.COST_RESIDENT:" + residentId);
        return states.lockForUpdate(BillingResidentState.KIND, residentId).orElseGet(() -> {
            var empty = BillingResidentState.empty();
            states.save(BillingResidentState.KIND, residentId, empty);
            return empty;
        });
    }

    public BillingResidentState statement(long residentId, long statementId) {
        var state = resident(residentId);
        if (!rows.statement(residentId, statementId)) throw new NotFoundException();
        return state;
    }
}
