package com.eldercare.backend.billing.internal.item.service;

import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.item.domain.CostRecalculation;
import com.eldercare.backend.billing.internal.item.persistence.*;
import com.eldercare.backend.billing.internal.period.domain.CostStatementRecord;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.PermissionDeniedException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CostQueries {
    private final CostStatementStore statements;
    private final CostItemStore items;
    private final CostViewStore views;
    private final CostAccess access;
    private final StatusHistoryApi history;
    private final CostItemHistory commands;
    private final com.eldercare.backend.billing.internal.period.persistence.CostPeriodStore periods;

    public CostQueries(
            CostStatementStore statements,
            CostItemStore items,
            CostViewStore views,
            CostAccess access,
            StatusHistoryApi history,
            CostItemHistory commands,
            com.eldercare.backend.billing.internal.period.persistence.CostPeriodStore periods) {
        this.statements = statements;
        this.items = items;
        this.views = views;
        this.access = access;
        this.history = history;
        this.commands = commands;
        this.periods = periods;
    }

    public record Statement(CostStatementView value, int version) {}

    public Statement statement(long id) {
        staff();
        var row = statements.statement(id);
        access.viewOrCommand(Function.COST_ADJUSTMENT, Operation.VIEW, "M11.COST_STATEMENT", id, row.residentId());
        return new Statement(view(row, true), row.version());
    }

    public List<CostStatementView> list(Long period, Long resident, String status) {
        staff();
        access.viewOrCommand(Function.COST_ADJUSTMENT, Operation.VIEW, "M11.COST_STATEMENT", 0, null);
        return statements.list(period, resident, status).stream()
                .map(s -> {
                    access.viewOrCommand(
                            Function.COST_ADJUSTMENT, Operation.VIEW, "M11.COST_STATEMENT", s.id(), s.residentId());
                    return view(s, false);
                })
                .toList();
    }

    public CostItemView item(long id) {
        staff();
        var row = items.item(id);
        access.viewOrCommand(Function.COST_ADJUSTMENT, Operation.VIEW, "M11.COST_ITEM", id, row.residentId());
        var v = views.item(id);
        var recalculations = history.list(CostRecalculation.TRACK, id).stream()
                .map(h -> {
                    Map<String, Object> fields = new LinkedHashMap<>();
                    fields.put("before", h.details().before());
                    fields.put("after", h.details().after());
                    fields.put("cause", h.details().cause());
                    fields.put("at", h.startsAt());
                    return fields;
                })
                .toList();
        return new CostItemView(
                v.id(),
                v.costType(),
                v.origin(),
                v.billingSource(),
                v.description(),
                v.itemCode(),
                null,
                v.incurredOn(),
                v.quantity(),
                v.unit(),
                v.unitPrice(),
                v.unitPriceBasis(),
                v.factor(),
                v.amount(),
                v.flags(),
                v.status(),
                v.originalItemId(),
                v.originalPeriod(),
                recalculations,
                commands.read(id),
                v.adjustments(),
                v.currentTotal(),
                v.hiddenFields());
    }

    public List<CostPeriodView> periods() {
        staff();
        access.viewOrCommand(Function.PERIOD_CLOSING, Operation.VIEW, "M11.COST_PERIOD", 0, null);
        return periods.views();
    }

    private CostStatementView view(CostStatementRecord row, boolean detail) {
        var totals = row.status().equals("FINALIZED")
                ? views.savedTotals(row.id())
                : statements.totals(row.id(), false).entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> new CostTypeTotal(e.getKey(), e.getValue()))
                        .toList();
        return new CostStatementView(
                row.id(),
                row.month().toString(),
                row.residentId(),
                CostStatementType.valueOf(row.statementType()),
                row.finalPeriod(),
                row.throughDate(),
                row.status(),
                totals,
                row.status().equals("FINALIZED")
                        ? row.total()
                        : totals.stream().map(CostTypeTotal::total).reduce(0L, Math::addExact),
                detail ? views.ofStatement(row.id()) : null);
    }

    private void staff() {
        if (access.user().roles().stream()
                .noneMatch(r -> Set.of(Role.OFFICE_STAFF, Role.NURSING_HOME_MANAGER, Role.ACCOUNTANT)
                        .contains(r)))
            throw new PermissionDeniedException("Chức năng dành cho nhân viên quản lý chi phí");
    }
}
