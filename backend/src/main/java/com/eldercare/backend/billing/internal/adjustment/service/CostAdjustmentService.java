package com.eldercare.backend.billing.internal.adjustment.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.billing.api.events.CostItemChanged;
import com.eldercare.backend.billing.internal.adjustment.domain.*;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.item.persistence.*;
import com.eldercare.backend.billing.internal.item.service.CostDestination;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.period.service.CostLock;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class CostAdjustmentService {
    private final CostItemStore items;
    private final CostCreationStore creation;
    private final CostStatementStore statements;
    private final CostRecalculationStore calculations;
    private final CostDestination destination;
    private final CostLock locks;
    private final CostAccess access;
    private final RecordEventApi changes;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    public CostAdjustmentService(
            CostItemStore items,
            CostCreationStore creation,
            CostStatementStore statements,
            CostRecalculationStore calculations,
            CostDestination destination,
            CostLock locks,
            CostAccess access,
            RecordEventApi changes,
            AuditLogApi audit,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.items = items;
        this.creation = creation;
        this.statements = statements;
        this.calculations = calculations;
        this.destination = destination;
        this.locks = locks;
        this.access = access;
        this.changes = changes;
        this.audit = audit;
        this.time = time;
        this.events = events;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.COST_ADJUSTMENT, operation = Operation.CREATE)
    public long adjust(long id, long amount, String type, Reason reason, Long changeId) {
        if (reason == null
                || type == null
                || !Set.of("POST_FINALIZATION_CORRECTION", "SOURCE_CHANGE", "WAIVER", "LATE_ADDITION")
                        .contains(type)) throw new InvalidValueException("Loại điều chỉnh và lý do bắt buộc");
        var original = items.item(id);
        long actor = access.require(
                Role.OFFICE_STAFF,
                Function.COST_ADJUSTMENT,
                Operation.CREATE,
                "M11.COST_ITEM",
                id,
                original.residentId());
        locks.resident(original.residentId());
        statements.lockPeriod(original.periodId());
        locks.statement(original.residentId(), original.statementId());
        original = items.lockedItem(id);
        if (original.status().equals("CANCELLED"))
            throw new BusinessException(HttpStatus.CONFLICT, "CONDITION_NOT_MET", "Cần chọn khoản gốc còn hiệu lực");
        if (changeId != null
                && !changes.require(PostFinalizationChange.KIND, changeId)
                        .parent()
                        .equals(new EntityRef("M11.COST_ITEM", id)))
            throw new InvalidValueException("Biến động không thuộc khoản gốc");
        try {
            CostAdjustmentRules.requireNonnegative(
                    original.amount() == null ? null : BigDecimal.valueOf(original.amount()),
                    creation.adjustmentTotal(id),
                    BigDecimal.valueOf(amount));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONDITION_NOT_MET", ex.getMessage());
        }
        var target = original.status().equals("FINALIZED")
                ? destination.adjustment(original.residentId(), actor)
                : statements.lockedStatement(original.statementId());
        long created = creation.adjustment(
                id, target.id(), target.periodId(), amount, type, reason.value(), changeId, actor, time.now());
        statements.touch(target.id(), true);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("COST_ADJUSTMENT_CREATE", true))
                .entity("M11.COST_ITEM", created)
                .resident(original.residentId())
                .after(Map.of("originalItemId", id, "amount", amount, "adjustmentType", type))
                .reason(reason)
                .build());
        events.publishEvent(new CostItemChanged(original.residentId(), target.periodId()));
        return created;
    }

    @Transactional(readOnly = true)
    public List<CostSourceChangeView> changes(Long resident) {
        access.require(Role.OFFICE_STAFF, Function.COST_ADJUSTMENT, Operation.VIEW, "M11.COST_ITEM", 0, resident);
        var result = new ArrayList<CostSourceChangeView>();
        for (long id : calculations.changeItems(resident)) {
            var item = items.item(id);
            access.viewOrCommand(Function.COST_ADJUSTMENT, Operation.VIEW, "M11.COST_ITEM", id, item.residentId());
            for (var change : changes.list(PostFinalizationChange.KIND, new EntityRef("M11.COST_ITEM", id))) {
                result.add(new CostSourceChangeView(
                        change.id(),
                        id,
                        item.residentId(),
                        change.content().event(),
                        change.content().before(),
                        change.content().after(),
                        change.content().reference(),
                        change.occurredAt()));
            }
        }
        return result;
    }
}
