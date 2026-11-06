package com.eldercare.backend.billing.internal.period.service;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.billing.internal.item.persistence.CostItemStore;
import com.eldercare.backend.billing.internal.period.domain.*;
import com.eldercare.backend.billing.internal.period.persistence.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T018: chỉ tạo kỳ của viện, chuyển trạng thái, gỡ dấu và nhắc; không lập bảng hoặc khoản. */
@Service
public class CostDailyTask implements CostTimedTask {
    private final CostPeriodStore store;
    private final CostBillingCoverage coverage;
    private final ParameterApi parameters;
    private final ReminderMarkers markers;
    private final CostNotifications notifications;
    private final CostStatementStore statements;
    private final CostItemStore items;
    private final CostLock locks;
    private final AuditLogApi audit;

    public CostDailyTask(
            CostPeriodStore store,
            CostBillingCoverage coverage,
            ParameterApi parameters,
            ReminderMarkers markers,
            CostNotifications notifications,
            CostStatementStore statements,
            CostItemStore items,
            CostLock locks,
            AuditLogApi audit) {
        this.store = store;
        this.coverage = coverage;
        this.parameters = parameters;
        this.markers = markers;
        this.notifications = notifications;
        this.statements = statements;
        this.items = items;
        this.locks = locks;
        this.audit = audit;
    }

    public String name() {
        return "COST_PERIOD_CALENDAR";
    }

    public CostJobCadence cadence() {
        return CostJobCadence.DAILY;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int run(Instant slot) {
        var today = LocalDate.ofInstant(slot, BusinessTime.ZONE);
        if (store.ensurePeriod(YearMonth.from(today), slot))
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("COST_PERIOD_OPENED", false))
                    .entity("M11.COST_PERIOD", store.periodId(YearMonth.from(today)))
                    .basis(new Basis("010.FR-001", null, null, MDC.get(SchedulerRuns.MDC_KEY)))
                    .after(Map.of("status", "OPEN"))
                    .build());
        int changed = 0;
        for (var item : store.advanceItems(today)) {
            locks.resident(item.residentId());
            statements.lockPeriod(item.periodId());
            locks.statement(item.residentId(), item.statementId());
            var current = items.lockedItem(item.id());
            if (Set.of("FINALIZED", "CANCELLED").contains(current.status())
                    || !current.flags().contains("CREATED_IN_ADVANCE")) continue;
            store.releaseAdvanceFlag(item.id());
            statements.touch(item.statementId(), false);
            var flags = new TreeSet<>(current.flags());
            flags.remove("CREATED_IN_ADVANCE");
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("COST_ADVANCE_RELEASED", false))
                    .entity("M11.COST_ITEM", item.id())
                    .basis(new Basis("010.FR-007", null, item.id(), MDC.get(SchedulerRuns.MDC_KEY)))
                    .resident(item.residentId())
                    .before(Map.of("flags", current.flags()))
                    .after(Map.of("flags", flags))
                    .build());
            changed++;
        }
        for (var period : store.periods()) {
            if (period.status().equals("FINALIZED")
                    || !today.isAfter(period.month().atEndOfMonth())) continue;
            LocalDate due = period.finalizationDueOn();
            if (period.status().equals("OPEN")) {
                var config = parameters.read(ParameterCode.CFG_M11_01).raw();
                var dueMonth = period.month()
                        .plusMonths(config.get("monthsAfterPeriodEnd").longValue());
                var day = config.get("dayOfMonth");
                due = day.isString() && day.asString().equals("LAST")
                        ? dueMonth.atEndOfMonth()
                        : dueMonth.atDay(Math.min(day.intValue(), dueMonth.lengthOfMonth()));
                if (store.closePeriod(period.id(), due, slot)) {
                    audit.write(AuditLogEntry.builder()
                            .action(new AuditAction("COST_PERIOD_AWAITING", false))
                            .entity("M11.COST_PERIOD", period.id())
                            .basis(new Basis("CFG-M11-01", null, null, MDC.get(SchedulerRuns.MDC_KEY)))
                            .before(Map.of("status", "OPEN"))
                            .after(Map.of("status", "AWAITING_FINALIZATION", "finalizationDueOn", due))
                            .build());
                    changed++;
                }
            }
            for (long resident : coverage.expected(period.month())) {
                if (!store.hasRegularStatement(period.id(), resident)
                        && markers.mark(
                                "M11.COST_REMINDER",
                                "M11.COST_PERIOD",
                                period.id(),
                                "MISSING:" + resident + ":" + today,
                                null)) {
                    notifications.staff(
                            "COST_STATEMENT_MISSING",
                            "M11.COST_PERIOD",
                            period.id(),
                            resident,
                            "M11:MISSING:" + period.id() + ":" + resident + ":" + today,
                            "Chưa có bảng chi phí thường của kỳ " + period.month(),
                            Role.OFFICE_STAFF,
                            slot,
                            null);
                    changed++;
                }
            }
            if (due != null && !today.isBefore(due))
                for (var s : statements.list(period.id(), null, null)) {
                    if (s.status().equals("FINALIZED")) continue;
                    var roles = EnumSet.of(s.status().equals("OPEN") ? Role.OFFICE_STAFF : Role.NURSING_HOME_MANAGER);
                    if (s.status().equals("OPEN")
                            && items.ofStatement(s.id()).stream()
                                    .anyMatch(i -> i.flags().contains("MISSING_UNIT_PRICE")))
                        roles.add(Role.NURSING_HOME_MANAGER);
                    for (var role : roles)
                        if (markers.mark(
                                "M11.COST_REMINDER", "M11.COST_STATEMENT", s.id(), "DUE:" + role + ":" + today, null)) {
                            notifications.staff(
                                    "COST_FINALIZATION_DUE",
                                    s.id(),
                                    s.residentId(),
                                    "M11:DUE:" + s.id() + ":" + role + ":" + today,
                                    "Bảng chi phí đã tới hạn chốt",
                                    role,
                                    slot,
                                    null);
                            changed++;
                        }
                }
        }
        return changed;
    }
}
