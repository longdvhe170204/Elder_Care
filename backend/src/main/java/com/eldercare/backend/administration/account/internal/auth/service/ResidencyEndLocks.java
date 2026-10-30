package com.eldercare.backend.administration.account.internal.auth.service;

import com.eldercare.backend.administration.account.internal.persistence.AccountStore;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Khóa tài khoản người thân khi mọi người cao tuổi có quan hệ đã quá CFG-M01-04 sau trạng thái cuối (002 T070; FR-049,
 * US6 #5, #6). Mỗi tài khoản một transaction ({@link AccountService#lockIfResidenciesExpired}), kiểm lại dưới khóa dòng
 * nên chạy lại không khóa hai lần; căn cứ CFG-M01-04 kèm lần chạy.
 */
@Component
public class ResidencyEndLocks {

    static final String JOB = "LOCK_AFTER_RESIDENCY_END";
    private static final Logger LOG = LoggerFactory.getLogger(ResidencyEndLocks.class);

    private final AccountStore store;
    private final AccountService accounts;
    private final SchedulerRuns runs;

    ResidencyEndLocks(AccountStore store, AccountService accounts, SchedulerRuns runs) {
        this.store = store;
        this.accounts = accounts;
        this.runs = runs;
    }

    /** Chạy một đợt, trả mã lần chạy (UUID). */
    public String run() {
        String runId = runs.start(JOB);
        Basis basis = new Basis("CFG-M01-04", null, null, runId);
        int processed = 0;
        boolean failed = false;
        for (long id : store.familyNotLocked()) {
            try {
                if (accounts.lockIfResidenciesExpired(id, basis)) {
                    processed++;
                }
            } catch (RuntimeException e) {
                failed = true;
                LOG.warn("Khóa sau kết thúc lưu trú tài khoản {} lỗi", id, e);
            }
        }
        runs.finish(runId, failed ? SchedulerRuns.Result.FAILED : SchedulerRuns.Result.SUCCEEDED, processed);
        return runId;
    }
}
