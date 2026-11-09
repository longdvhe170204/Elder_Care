package com.eldercare.backend.billing.internal.source.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.billing.api.events.CostItemChanged;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.item.persistence.CostCreationStore;
import com.eldercare.backend.billing.internal.item.service.CostDestination;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.period.service.*;
import com.eldercare.backend.billing.internal.pricing.domain.CalculatedCost;
import com.eldercare.backend.billing.internal.pricing.service.SourceCostPricing;
import com.eldercare.backend.billing.internal.source.domain.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class SourceCostCommands {
    private final CostSourceLookup lookup;
    private final BillingSourceSelection selection;
    private final CostAccess access;
    private final CostLock locks;
    private final CostDestination destination;
    private final CostCreationStore creation;
    private final CostStatementStore statements;
    private final SourceCostPricing pricing;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final CostNotifications notifications;
    private final com.eldercare.backend.billing.internal.finalperiod.service.FinalBillingWindow window;

    public SourceCostCommands(
            CostSourceLookup lookup,
            BillingSourceSelection selection,
            CostAccess access,
            CostLock locks,
            CostDestination destination,
            CostCreationStore creation,
            CostStatementStore statements,
            SourceCostPricing pricing,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            CostNotifications notifications,
            com.eldercare.backend.billing.internal.finalperiod.service.FinalBillingWindow window) {
        this.lookup = lookup;
        this.selection = selection;
        this.access = access;
        this.locks = locks;
        this.destination = destination;
        this.creation = creation;
        this.statements = statements;
        this.pricing = pricing;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.notifications = notifications;
        this.window = window;
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.COST_ADJUSTMENT, operation = Operation.CREATE)
    public SourceCostResult create(SourceCostSelection request) {
        validate(request);
        long actor = access.require(
                Role.OFFICE_STAFF,
                Function.COST_ADJUSTMENT,
                Operation.CREATE,
                "M01.RESIDENT",
                request.residentId(),
                request.residentId());
        locks.resident(request.residentId());
        var skipped = new ArrayList<SourceCostResult.Skipped>();
        var sources = selected(request, skipped);
        var succeeded = new ArrayList<Long>();
        for (var source : sources) {
            String key = source.sourceId() + ":" + source.billingUnit();
            if (source.existingItemId() != null) {
                skipped.add(new SourceCostResult.Skipped(
                        key, "COST_ITEM_ALREADY_EXISTS", "Đơn vị nguồn đã có khoản hiệu lực"));
                continue;
            }
            if (source.blockedCode() != null) {
                skipped.add(new SourceCostResult.Skipped(key, source.blockedCode(), source.blockedReason()));
                continue;
            }
            boolean advance = !source.date().isBefore(time.today());
            if (advance) {
                if (!source.sourceType().equals("RESIDENCY_DAY")
                        || !window.advanceAllowed(source.residentId(), source.date())) {
                    skipped.add(new SourceCostResult.Skipped(
                            key, "CONDITION_NOT_MET", "Ngày chưa kết thúc và không thuộc phạm vi lập trước kỳ cuối"));
                    continue;
                }
            }
            var finalStatement = destination.finalizedFinalPeriod(source.residentId());
            var target = finalStatement.isPresent()
                    ? destination.adjustment(source.residentId(), actor)
                    : destination.regular(source.residentId(), YearMonth.from(source.date()), actor);
            Long originalPeriod = null;
            if (finalStatement.isPresent()) {
                originalPeriod = finalStatement.get().periodId();
            } else if (target.status().equals("FINALIZED")) {
                originalPeriod = target.periodId();
                target = destination.adjustment(source.residentId(), actor);
            }
            var cost = pricing.calculate(source);
            var flags = new LinkedHashSet<>(cost.flags());
            if (advance) flags.add("CREATED_IN_ADVANCE");
            if (originalPeriod != null) flags.add("AFTER_FINALIZED_PERIOD");
            if (finalStatement.isPresent()) {
                var closedThrough = finalStatement.get().throughDate() == null
                        ? finalStatement.get().month().atEndOfMonth()
                        : finalStatement.get().throughDate();
                if (source.date().isAfter(closedThrough)) flags.add("AFFECTS_TERMINATION");
            }
            cost = new CalculatedCost(
                    cost.quantity(),
                    cost.unitPrice(),
                    cost.factor(),
                    cost.amount(),
                    cost.basis(),
                    cost.sourceValues(),
                    flags);
            long id = creation.source(source, cost, target.id(), target.periodId(), originalPeriod, actor, time.now());
            statements.touch(target.id(), true);
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("COST_ITEM_CREATE", false))
                    .entity("M11.COST_ITEM", id)
                    .resident(source.residentId())
                    .after(Map.of("source", key, "calculation", cost))
                    .build());
            if (flags.contains("MISSING_UNIT_PRICE"))
                notifications.staff(
                        "MISSING_UNIT_PRICE",
                        target.id(),
                        source.residentId(),
                        "M11.MISSING_PRICE:" + id,
                        "Khoản chi phí thiếu đơn giá",
                        Role.OFFICE_STAFF,
                        time.now(),
                        actor);
            events.publishEvent(new CostItemChanged(source.residentId(), target.periodId()));
            succeeded.add(id);
        }
        return new SourceCostResult(List.copyOf(succeeded), List.copyOf(skipped));
    }

    private List<CostSourceCandidate> selected(SourceCostSelection r, List<SourceCostResult.Skipped> skipped) {
        if (r.from() != null) return lookup.find(r.residentId(), r.sourceType(), r.from(), r.to());
        if (Set.of("RESIDENCY_DAY", "DAY_CARE_SESSION").contains(r.sourceType())) {
            // Với ngày lưu trú, id đã được công bố là residentId:yyyy-MM-dd; không dùng thời gian hiện tại để đoán
            // nguồn.
            if (!r.sourceType().equals("RESIDENCY_DAY"))
                throw new InvalidValueException("Nguồn bán trú chưa có cổng tra đơn vị theo id");
            var keys = r.sourceUnits() != null
                    ? r.sourceUnits()
                    : r.sourceIds().stream()
                            .map(id -> new BillingSourceKey(id, "DAY"))
                            .toList();
            var result = new ArrayList<CostSourceCandidate>();
            for (var key : keys.stream().distinct().toList()) {
                try {
                    if (!key.sourceId().startsWith(r.residentId() + ":") || !"DAY".equals(key.billingUnit()))
                        throw new IllegalArgumentException();
                    var date = LocalDate.parse(
                            key.sourceId().substring(key.sourceId().indexOf(':') + 1));
                    var found = lookup.find(r.residentId(), r.sourceType(), date, date);
                    if (found.isEmpty()) throw new IllegalArgumentException();
                    result.addAll(found);
                } catch (IllegalArgumentException ex) {
                    skipped.add(new SourceCostResult.Skipped(
                            key.sourceId(), "NOT_A_BILLABLE_EVENT", "Không tìm thấy đơn vị nguồn"));
                }
            }
            return result;
        }
        var selected = r.sourceUnits() == null
                ? selection.byIds(r.residentId(), r.sourceType(), r.sourceIds())
                : selection.byUnits(
                        r.residentId(),
                        r.sourceType(),
                        r.sourceUnits().stream()
                                .map(k -> new BillingSourceSelection.SourceUnit(k.sourceId(), k.billingUnit()))
                                .toList());
        selected.skipped()
                .forEach(s ->
                        skipped.add(new SourceCostResult.Skipped(s.source().sourceId(), s.errorCode(), s.reason())));
        return selected.selected().stream().map(lookup::indexed).toList();
    }

    private void validate(SourceCostSelection r) {
        int choices = (r.sourceIds() == null ? 0 : 1)
                + (r.sourceUnits() == null ? 0 : 1)
                + (r.from() == null && r.to() == null ? 0 : 1);
        if (r.residentId() <= 0
                || r.sourceType() == null
                || !Set.of(
                                "RESIDENCY_DAY",
                                "DAY_CARE_SESSION",
                                "CARE_TASK_RESULT",
                                "DOSE",
                                "PRN_ADMINISTRATION",
                                "DISPENSE_RECORD",
                                "FAMILY_MEAL",
                                "OVERNIGHT_STAY",
                                "ACTIVITY_ATTENDANCE")
                        .contains(r.sourceType())
                || choices != 1
                || r.sourceIds() != null
                        && (r.sourceIds().isEmpty() || r.sourceIds().stream().anyMatch(s -> s == null || s.isBlank()))
                || r.sourceUnits() != null
                        && (r.sourceUnits().isEmpty()
                                || r.sourceUnits().stream()
                                        .anyMatch(s -> s == null
                                                || s.sourceId() == null
                                                || s.sourceId().isBlank()
                                                || s.billingUnit() == null
                                                || s.billingUnit().isBlank()))
                || (r.from() != null || r.to() != null)
                        && (!"RESIDENCY_DAY".equals(r.sourceType())
                                || r.from() == null
                                || r.to() == null
                                || r.from().isAfter(r.to())))
            throw new InvalidValueException(
                    "Chọn đúng một danh sách nguồn, đơn vị nguồn hoặc khoảng ngày lưu trú hợp lệ");
    }
}
