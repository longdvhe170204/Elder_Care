package com.eldercare.backend.billing.internal.finalperiod.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.admission.api.*;
import com.eldercare.backend.admission.api.dto.*;
import com.eldercare.backend.admission.api.spi.*;
import com.eldercare.backend.billing.api.FinalPeriodCostApi;
import com.eldercare.backend.billing.api.dto.FinalPeriodConditionView;
import com.eldercare.backend.billing.internal.finalperiod.domain.FinalPeriodCondition;
import com.eldercare.backend.billing.internal.finalperiod.persistence.FinalPeriodStore;
import com.eldercare.backend.billing.internal.item.persistence.CostRecalculationStore;
import com.eldercare.backend.billing.internal.item.service.*;
import com.eldercare.backend.billing.internal.period.domain.*;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.period.service.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T024: tác động kỳ cuối luôn nằm trong transaction của lệnh hồ sơ 004/001. */
@Service
public class FinalPeriodService implements FinalPeriodPort, FinalPeriodCostApi, PostDeathItemPort {
    private final CostLock locks;
    private final ResidentModuleStateApi states;
    private final CostDestination destination;
    private final CostStatementStore statements;
    private final com.eldercare.backend.billing.internal.period.persistence.CostPeriodStore periods;
    private final FinalPeriodStore store;
    private final ResidencyDayApi days;
    private final ResidentApi residents;
    private final CostRecalculationStore calculations;
    private final CostRecalculationService recalculation;
    private final BusinessTime time;
    private final CurrentUser user;
    private final CostNotifications notifications;
    private final AuditLogApi audit;
    private final com.eldercare.backend.common.data.ReminderMarkers markers;

    public FinalPeriodService(
            CostLock locks,
            ResidentModuleStateApi states,
            CostDestination destination,
            CostStatementStore statements,
            FinalPeriodStore store,
            ResidencyDayApi days,
            ResidentApi residents,
            CostRecalculationStore calculations,
            CostRecalculationService recalculation,
            BusinessTime time,
            CurrentUser user,
            CostNotifications notifications,
            AuditLogApi audit,
            com.eldercare.backend.billing.internal.period.persistence.CostPeriodStore periods,
            com.eldercare.backend.common.data.ReminderMarkers markers) {
        this.locks = locks;
        this.states = states;
        this.destination = destination;
        this.statements = statements;
        this.store = store;
        this.days = days;
        this.residents = residents;
        this.calculations = calculations;
        this.recalculation = recalculation;
        this.time = time;
        this.user = user;
        this.notifications = notifications;
        this.audit = audit;
        this.periods = periods;
        this.markers = markers;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long createDraft(long resident, LocalDate end) {
        return change(resident, end, null);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recalculate(long resident, LocalDate end) {
        change(resident, end, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void death(long resident, Instant deathAt) {
        change(resident, LocalDate.ofInstant(deathAt, BusinessTime.ZONE), deathAt);
    }

    private long change(long resident, LocalDate end, Instant death) {
        Objects.requireNonNull(end);
        var old = locks.resident(resident);
        if (old.finalStatementId() != null
                && end.equals(old.throughDate())
                && (death == null || death.equals(old.deathAt()))) return old.finalStatementId();
        var previous = old.finalStatementId() == null ? null : statements.statement(old.finalStatementId());
        var periodIds = new TreeSet<Long>();
        if (previous != null) periodIds.add(previous.periodId());
        if (previous == null || !previous.status().equals("FINALIZED")) {
            periods.ensurePeriod(YearMonth.from(end), time.now());
            periodIds.add(periods.periodId(YearMonth.from(end)));
        }
        calculations.affected(resident, null, end.plusDays(1), null, false).forEach(i -> periodIds.add(i.periodId()));
        periodIds.forEach(statements::lockPeriod);
        CostStatementRecord target;
        if (previous != null && previous.status().equals("FINALIZED")) {
            statements.lockPeriod(previous.periodId());
            target = statements.lockedStatement(previous.id());
        } else {
            var month = YearMonth.from(end);
            target = destination.regular(resident, month, user.accountId());
            if (previous != null && previous.id() != target.id()) {
                statements.lockPeriod(previous.periodId());
                store.mark(previous.id(), null);
            }
            store.mark(target.id(), end);
        }
        var next = new BillingResidentState(
                old.finalCostCompletedFrom(),
                target.id(),
                end,
                death == null ? old.deathAt() : death,
                old.revision() + 1);
        states.save(BillingResidentState.KIND, resident, next);
        cancelFutureDays(resident, end, death == null ? "END_DATE_CHANGED" : "RECORD_DEATH", next.revision());
        notice(resident, target.id(), next.revision(), "Kỳ cuối tính đến ngày " + end);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("FINAL_PERIOD_CHANGED", false))
                .entity("M11.COST_STATEMENT", target.id())
                .resident(resident)
                .before(Map.of("finalPeriod", old))
                .after(Map.of("finalPeriod", next))
                .build());
        return target.id();
    }

    private void cancelFutureDays(long resident, LocalDate end, String cause, long revision) {
        var rows = calculations.affected(resident, null, end.plusDays(1), null, false).stream()
                .filter(i -> Set.of("RESIDENCY_DAY", "DAY_CARE_SESSION").contains(i.sourceType()))
                .toList();
        rows.stream().map(i -> i.periodId()).distinct().sorted().forEach(statements::lockPeriod);
        for (var i : rows)
            recalculation.sourceChanged(
                    i.sourceType(),
                    i.sourceId(),
                    i.billingUnit(),
                    null,
                    true,
                    cause,
                    "M11.END:" + resident + ":" + revision);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelAfter(long resident, LocalDate end, String cause, long revision) {
        locks.resident(resident);
        cancelFutureDays(resident, end, cause, revision);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelStop(long resident) {
        var old = locks.resident(resident);
        if (old.finalStatementId() == null) return;
        var s = statements.statement(old.finalStatementId());
        statements.lockPeriod(s.periodId());
        statements.lockedStatement(s.id());
        store.mark(s.id(), null);
        var next =
                new BillingResidentState(old.finalCostCompletedFrom(), null, null, old.deathAt(), old.revision() + 1);
        states.save(BillingResidentState.KIND, resident, next);
        notice(resident, s.id(), next.revision(), "Đã bỏ mốc dừng kỳ cuối; cần tra cứu nguồn để lập chi phí");
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("FINAL_PERIOD_STOP_CANCELLED", false))
                .entity("M11.COST_STATEMENT", s.id())
                .resident(resident)
                .before(Map.of("finalPeriod", old))
                .after(Map.of("finalPeriod", next))
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public FinalPeriodConditionView finalPeriodCondition(long resident) {
        var state = states.get(BillingResidentState.KIND, resident).orElseGet(BillingResidentState::empty);
        if (state.finalStatementId() != null
                && !org.springframework.transaction.support.TransactionSynchronizationManager
                        .isCurrentTransactionReadOnly()) state = locks.resident(resident);
        if (state.finalStatementId() == null || state.throughDate() == null)
            return new FinalPeriodConditionView(false, List.of("Chưa có bảng kỳ cuối"));
        var s = statements.statement(state.finalStatementId());
        LocalDate through = state.throughDate();
        if (!residents.isFinalStatus(resident) && through.isBefore(time.today().minusDays(1)))
            through = time.today().minusDays(1);
        if (state.deathAt() != null) through = LocalDate.ofInstant(state.deathAt(), BusinessTime.ZONE);
        var required = new HashSet<LocalDate>();
        var from =
                s.month().atDay(1).isAfter(YearMonth.from(state.throughDate()).atDay(1))
                        ? YearMonth.from(state.throughDate()).atDay(1)
                        : s.month().atDay(1);
        for (var d : days.daysBetween(resident, from, through)) if (!d.bedReleased()) required.add(d.date());
        var reasons = FinalPeriodCondition.reasons(
                s.status().equals("FINALIZED"), required, store.covered(resident), store.pendingAdjustment(resident));
        return new FinalPeriodConditionView(reasons.isEmpty(), reasons);
    }

    @Override
    public PostDeathItemCode code() {
        return PostDeathItemCode.FINAL_PERIOD_COSTS;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PostDeathItemStatusView status(long resident) {
        var state = locks.resident(resident);
        if (state.finalCostCompletedFrom() != null) return completed();
        boolean complete = state.deathAt() != null
                && state.finalStatementId() != null
                && statements.list(null, resident, null).stream()
                        .filter(s -> s.id() == state.finalStatementId()
                                || s.statementType().equals("SUPPLEMENTARY"))
                        .allMatch(s -> s.status().equals("FINALIZED"));
        if (!complete)
            return new PostDeathItemStatusView(
                    PostDeathItemStatus.NOT_COMPLETED, "Còn bảng kỳ cuối hoặc bảng bổ sung chưa chốt");
        states.save(
                BillingResidentState.KIND,
                resident,
                new BillingResidentState(
                        time.now(), state.finalStatementId(), state.throughDate(), state.deathAt(), state.revision()));
        return completed();
    }

    private PostDeathItemStatusView completed() {
        return new PostDeathItemStatusView(
                PostDeathItemStatus.COMPLETED, "Đã chốt kỳ cuối và các bảng bổ sung hiện có");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recheck(long resident) {
        var existing = states.get(BillingResidentState.KIND, resident);
        if (existing.isEmpty() || existing.get().finalStatementId() == null) return;
        var state = locks.resident(resident);
        if (state.finalStatementId() == null) return;
        var condition = finalPeriodCondition(resident);
        if (!condition.satisfied()
                && markers.mark(
                        "M11.COST_REMINDER",
                        "M11.COST_STATEMENT",
                        state.finalStatementId(),
                        "CONDITION:" + state.revision() + ":" + time.today(),
                        null))
            notifications.staff(
                    "FINAL_PERIOD_CONDITION_UNMET",
                    state.finalStatementId(),
                    resident,
                    "M11.END_CONDITION:" + resident + ":" + state.revision() + ":" + time.today(),
                    String.join("; ", condition.reasons()),
                    Role.OFFICE_STAFF,
                    time.now(),
                    null);
    }

    private void notice(long resident, long statement, long revision, String text) {
        notifications.staff(
                "FINAL_PERIOD_CHANGED",
                statement,
                resident,
                "M11.END:" + resident + ":" + revision,
                text,
                Role.OFFICE_STAFF,
                time.now(),
                user.isScheduler() ? null : user.accountId());
    }
}
