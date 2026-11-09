package com.eldercare.backend.billing.balance.internal.status.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.billing.api.dto.BalanceStatus;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceDetails;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.ledger.domain.LedgerState;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.resident.api.ResidentApi;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh sách Sắp hết tiền, Còn nợ (Kế toán, Quản lý viện; FR-016 → FR-018) và lệnh "dừng nhắc" của Quản lý viện cho hồ
 * sơ trạng thái cuối còn nợ (FR-020; chú thích ³² của dòng "Số dư, thu chi").
 */
@Service
public class BalanceStatusCommands {

    static final AuditAction STOPPED = new AuditAction("BALANCE_REMINDERS_STOPPED", true);
    private static final Set<Role> LIST_ROLES = Set.of(Role.ACCOUNTANT, Role.NURSING_HOME_MANAGER);

    private final LedgerStore ledgers;
    private final BalanceQueries queries;
    private final ResidentApi residents;
    private final AuditLogApi audit;
    private final CurrentUser user;

    public BalanceStatusCommands(
            LedgerStore ledgers, BalanceQueries queries, ResidentApi residents, AuditLogApi audit, CurrentUser user) {
        this.ledgers = ledgers;
        this.queries = queries;
        this.residents = residents;
        this.audit = audit;
        this.user = user;
    }

    @RequiresPermission(function = Function.BALANCE_LEDGER, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<BalanceDetails> statuses(BalanceStatus status) {
        if (user.roles().stream().noneMatch(LIST_ROLES::contains)) {
            throw new PermissionDeniedException("Danh sách tình trạng số dư chỉ cho Kế toán, Quản lý viện");
        }
        Set<BalanceStatus> wanted =
                status == null ? EnumSet.of(BalanceStatus.RUNNING_LOW, BalanceStatus.IN_DEBT) : EnumSet.of(status);
        wanted.remove(BalanceStatus.NORMAL);
        return ledgers.inStatuses(wanted).stream().map(queries::details).toList();
    }

    @RequiresPermission(function = Function.BALANCE_LEDGER, operation = Operation.APPROVE)
    @BusinessCommand
    @Transactional
    public void stopReminders(Target target, long residentId, Reason reason) {
        LedgerState ledger = ledgers.lock(residentId).orElseThrow(NotFoundException::new);
        if (!residents.isFinalStatus(residentId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "NOT_FINAL_STATUS", "Chỉ dừng nhắc cho hồ sơ đã ở trạng thái cuối (FR-020)");
        }
        if (ledger.balance() >= 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Hồ sơ không còn nợ");
        }
        if (ledger.remindersStopped()) return;
        ledgers.stopReminders(residentId, user.accountId(), reason.value());
        audit.write(AuditLogEntry.builder()
                .action(STOPPED)
                .entity(BalanceTypes.LEDGER, residentId)
                .resident(residentId)
                .before(Map.of("remindersStopped", false))
                .after(Map.of("remindersStopped", true))
                .reason(reason)
                .build());
    }
}
