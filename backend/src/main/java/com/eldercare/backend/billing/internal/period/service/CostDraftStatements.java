package com.eldercare.backend.billing.internal.period.service;

import com.eldercare.backend.billing.internal.period.domain.DraftStatementTarget;
import com.eldercare.backend.billing.internal.period.domain.DraftStatementTransition;
import com.eldercare.backend.billing.internal.period.persistence.DraftCostStatementStore;
import com.eldercare.backend.common.time.BusinessTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * T011: bước nội bộ của lệnh lập khoản, gọi sau kiểm nguồn và cùng transaction ghi khoản/nhật ký.
 * Không phải lệnh công khai; không gọi từ job hoặc riêng để lập bảng trống.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class CostDraftStatements {
    private final CostLock lock;
    private final DraftCostStatementStore statements;
    private final BusinessTime time;

    public CostDraftStatements(CostLock lock, DraftCostStatementStore statements, BusinessTime time) {
        this.lock = lock;
        this.statements = statements;
        this.time = time;
    }

    public DraftStatementTarget receiveNewDraft(long residentId, long periodId, long actorId) {
        lock.resident(residentId);
        var periodStatus = statements.periodStatusForShare(periodId);
        var existing = statements.regularForUpdate(residentId, periodId);
        var transition = DraftStatementTransition.forNewDraft(periodStatus, existing.map(row -> row.status()).orElse(null));
        if (transition.requiresAdjustment()) {
            return new DraftStatementTarget(existing.orElse(null), false, false, true);
        }
        if (transition.createStatement()) {
            return new DraftStatementTarget(statements.createRegular(residentId, periodId, actorId, time.now()),
                    true, false, false);
        }
        var target = existing.orElseThrow();
        if (transition.reopenStatement()) {
            statements.reopen(residentId, target.id());
            target = statements.regularForUpdate(residentId, periodId).orElseThrow();
        }
        return new DraftStatementTarget(target, false, transition.reopenStatement(), false);
    }
}
