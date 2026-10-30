package com.eldercare.backend.administration.account.internal.auth.service;

import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.internal.auth.domain.AccountAuditActions;
import com.eldercare.backend.administration.account.internal.domain.Account;
import com.eldercare.backend.administration.account.internal.persistence.AccountStore;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Chuyển tài khoản người thân không đăng nhập quá CFG-M15-02 sang Không hoạt động (002 T043; FR-010, FR-013;
 * BR-M15-05; NFR-04, NFR-05). Mỗi tài khoản một transaction, kiểm lại trên trạng thái hiện hành dưới khóa dòng nên chạy
 * lại không chuyển hai lần; kết thúc phiên; nhật ký Bộ lập lịch kèm căn cứ CFG-M15-02 và lần chạy.
 */
@Component
public class InactiveAccounts {

    static final String JOB = "MARK_INACTIVE_ACCOUNTS";
    private static final Logger LOG = LoggerFactory.getLogger(InactiveAccounts.class);

    private final AccountStore store;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final AuditLogApi auditLog;
    private final SchedulerRuns runs;
    private final TransactionTemplate transaction;

    InactiveAccounts(
            AccountStore store,
            BusinessTime time,
            ParameterApi parameters,
            AuditLogApi auditLog,
            SchedulerRuns runs,
            TransactionTemplate transaction) {
        this.store = store;
        this.time = time;
        this.parameters = parameters;
        this.auditLog = auditLog;
        this.runs = runs;
        this.transaction = transaction;
    }

    /** Chạy một đợt, trả mã lần chạy (UUID). */
    public String run() {
        String runId = runs.start(JOB);
        Instant cutoff =
                time.now().minus(parameters.read(ParameterCode.CFG_M15_02).asDuration());
        int processed = 0;
        boolean failed = false;
        for (long id : store.familyActiveSince(cutoff)) {
            try {
                if (Boolean.TRUE.equals(transaction.execute(s -> markInactive(id, cutoff, runId)))) {
                    processed++;
                }
            } catch (RuntimeException e) {
                failed = true;
                LOG.warn("Chuyển Không hoạt động tài khoản {} lỗi", id, e);
            }
        }
        runs.finish(runId, failed ? SchedulerRuns.Result.FAILED : SchedulerRuns.Result.SUCCEEDED, processed);
        return runId;
    }

    private boolean markInactive(long accountId, Instant cutoff, String runId) {
        Account account = store.lock(accountId).orElseThrow();
        if (account.type() != AccountType.FAMILY_MEMBER
                || account.status() != AccountStatus.ACTIVE
                || !store.inactivityReference(accountId).isBefore(cutoff)) {
            return false;
        }
        store.changeStatus(accountId, AccountStatus.INACTIVE, null, null);
        store.endSessions(accountId, "ACCOUNT_LEFT_ACTIVE");
        auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.ACCOUNT_INACTIVE)
                .entity(AccountAuditActions.ENTITY, accountId)
                .before(Map.of("status", AccountStatus.ACTIVE.name()))
                .after(Map.of("status", AccountStatus.INACTIVE.name()))
                .basis(new Basis("CFG-M15-02", null, null, runId))
                .build());
        return true;
    }
}
