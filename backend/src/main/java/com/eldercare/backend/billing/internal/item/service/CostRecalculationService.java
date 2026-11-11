package com.eldercare.backend.billing.internal.item.service;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.shared.api.*;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.billing.api.events.CostItemChanged;
import com.eldercare.backend.billing.internal.adjustment.domain.PostFinalizationChange;
import com.eldercare.backend.billing.internal.item.domain.*;
import com.eldercare.backend.billing.internal.item.persistence.*;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.period.service.*;
import com.eldercare.backend.billing.internal.pricing.domain.CalculatedCost;
import com.eldercare.backend.billing.internal.pricing.service.*;
import com.eldercare.backend.billing.internal.source.service.CostSourceLookup;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** BR-M11-05: listener chỉ xử lý khoản đã tồn tại; không tự lập khoản hoặc điều chỉnh. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class CostRecalculationService {
    private final CostRecalculationStore store;
    private final CostItemStore items;
    private final CostStatementStore statements;
    private final CostLock locks;
    private final CostPricingService pricing;
    private final SourceCostPricing sourcePricing;
    private final CostSourceLookup lookup;
    private final StatusHistoryApi history;
    private final RecordEventApi changes;
    private final CostNotifications notifications;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final tools.jackson.databind.json.JsonMapper json;

    public CostRecalculationService(
            CostRecalculationStore store,
            CostItemStore items,
            CostStatementStore statements,
            CostLock locks,
            CostPricingService pricing,
            SourceCostPricing sourcePricing,
            CostSourceLookup lookup,
            StatusHistoryApi history,
            RecordEventApi changes,
            CostNotifications notifications,
            AuditLogApi audit,
            BusinessTime time,
            ApplicationEventPublisher events,
            tools.jackson.databind.json.JsonMapper json) {
        this.store = store;
        this.items = items;
        this.statements = statements;
        this.locks = locks;
        this.pricing = pricing;
        this.sourcePricing = sourcePricing;
        this.lookup = lookup;
        this.history = history;
        this.changes = changes;
        this.notifications = notifications;
        this.audit = audit;
        this.time = time;
        this.events = events;
        this.json = json;
    }

    public void sourceChanged(
            String type,
            String id,
            String unit,
            BigDecimal quantity,
            boolean cancelled,
            String cause,
            String eventKey) {
        for (var item : store.source(type, id, unit)) {
            var facts = new LinkedHashMap<String, Object>();
            facts.put("sourceType", type);
            facts.put("sourceId", id);
            facts.put("billingUnit", unit);
            facts.put("quantity", quantity == null ? item.calculation().quantity() : quantity);
            facts.put("cancelled", cancelled);
            apply(item, quantity, cancelled, facts, cause, eventKey);
        }
    }

    public void refresh(Long resident, Long service, LocalDate from, LocalDate to, boolean missingOnly, String cause) {
        refresh(resident, service, from, to, missingOnly, cause, null);
    }

    public void calendar(Long resident, LocalDate from, LocalDate to, String type, String cause) {
        refresh(resident, null, from, to, false, cause, type);
    }

    private void refresh(
            Long resident, Long service, LocalDate from, LocalDate to, boolean missingOnly, String cause, String type) {
        var found = new ArrayList<>(store.affected(resident, service, from, to, missingOnly));
        // Q3: khoản chưa xác định được dịch vụ cũng được tính lại khi có phiên bản đơn giá mới.
        if (service != null && missingOnly)
            store.affected(resident, null, from, to, true).stream()
                    .filter(row -> row.serviceId() == null)
                    .forEach(found::add);
        var rows = found.stream()
                .filter(row -> type == null || type.equals(row.sourceType()))
                .toList();
        rows.stream().map(StoredCostCalculation::residentId).distinct().sorted().forEach(locks::resident);
        rows.stream().map(StoredCostCalculation::periodId).distinct().sorted().forEach(statements::lockPeriod);
        for (var selected : rows) {
            var row = lockCurrent(selected);
            if (row.status().equals("CANCELLED")
                    || missingOnly
                            && (row.status().equals("FINALIZED")
                                    || row.calculation().amount() != null)) continue;
            // Nguồn lịch đọc lại; dữ liệu thiếu không được coi là nguồn đã hủy.
            if (Set.of("RESIDENCY_DAY", "DAY_CARE_SESSION").contains(row.sourceType())) {
                var source = lookup.find(row.residentId(), row.sourceType(), row.date(), row.date()).stream()
                        .filter(s -> s.sourceId().equals(row.sourceId())
                                && s.billingUnit().equals(row.billingUnit()))
                        .findFirst();
                if (source.isEmpty())
                    throw new IllegalStateException("Nguồn lịch không còn trả đơn vị đã tính phí; cần đối chiếu nguồn");
                var s = source.get();
                boolean cancelled = "NOT_A_BILLABLE_EVENT".equals(s.blockedCode())
                        || "OUTSIDE_BILLING_PERIOD".equals(s.blockedCode());
                if (s.blockedCode() != null && !cancelled) throw new IllegalStateException(s.blockedReason());
                apply(row, s.quantity(), cancelled, s.facts(), cause, null);
            } else apply(row, row.calculation().quantity(), false, row.sourceValues(), cause, null);
        }
    }

    private void apply(
            StoredCostCalculation selected,
            BigDecimal quantity,
            boolean cancelled,
            Map<String, Object> facts,
            String cause,
            String key) {
        var item = lockCurrent(selected);
        if (item.status().equals("CANCELLED")) return;
        if (item.status().equals("FINALIZED")) {
            var parent = new EntityRef("M11.COST_ITEM", item.id());
            String unique = key == null ? null : "M11.CHANGE:" + item.id() + ":" + key;
            if (unique != null
                    && changes.findByUniqueKey(PostFinalizationChange.KIND, unique)
                            .isPresent()) return;
            var previous = changes.list(PostFinalizationChange.KIND, parent);
            var before = previous.isEmpty()
                    ? item.sourceValues()
                    : previous.getLast().content().after();
            if (key == null && json.valueToTree(before).equals(json.valueToTree(facts))) return;
            var content = new PostFinalizationChange(cause, before, facts, key == null ? cause : key, time.now());
            var change = changes.add(
                    PostFinalizationChange.KIND,
                    parent,
                    new NewRecordEvent<>(content, null, null, true, time.now(), null, time.now(), null, unique));
            notifications.staff(
                    "POST_FINALIZATION_CHANGE",
                    "M11.COST_ITEM",
                    item.id(),
                    item.residentId(),
                    "M11.CHANGE:" + change.id(),
                    "Nguồn của khoản đã chốt thay đổi; cần xem xét lập điều chỉnh",
                    Role.OFFICE_STAFF,
                    time.now(),
                    null);
            audit(item, "COST_SOURCE_CHANGE", before, facts);
            return;
        }
        CalculatedCost cost = null;
        if (!cancelled) {
            if (Set.of("RESIDENCY_DAY", "DAY_CARE_SESSION").contains(item.sourceType())) {
                var source = lookup.find(item.residentId(), item.sourceType(), item.date(), item.date()).stream()
                        .filter(s -> s.sourceId().equals(item.sourceId())
                                && s.billingUnit().equals(item.billingUnit()))
                        .findFirst()
                        .orElseThrow();
                cost = sourcePricing.calculate(source);
                var flags = new LinkedHashSet<>(items.item(item.id()).flags());
                flags.removeAll(Set.of("MISSING_UNIT_PRICE", "INCLUDED_IN_PACKAGE", "NOT_YET_MOVED_IN"));
                if (item.date().isBefore(time.today())) flags.remove("CREATED_IN_ADVANCE");
                flags.addAll(cost.flags());
                cost = new CalculatedCost(
                        cost.quantity(),
                        cost.unitPrice(),
                        cost.factor(),
                        cost.amount(),
                        cost.basis(),
                        cost.sourceValues(),
                        flags);
            } else {
                Long service = item.serviceId() != null
                        ? item.serviceId()
                        : lookup.serviceForUnresolved(item.sourceType()).orElse(null);
                var quote = service == null
                        ? new CostPricingService.PriceQuote(null, null, false, null)
                        : pricing.event(item.residentId(), service, item.date(), quantity);
                var flags = new LinkedHashSet<>(items.item(item.id()).flags());
                flags.remove("MISSING_UNIT_PRICE");
                flags.remove("INCLUDED_IN_PACKAGE");
                if (quote.missingUnitPrice()) flags.add("MISSING_UNIT_PRICE");
                if (quote.inPackage()) flags.add("INCLUDED_IN_PACKAGE");
                cost = new CalculatedCost(
                        quantity,
                        quote.unitPrice(),
                        item.calculation().factorPercent(),
                        quote.amount(),
                        quote.basis(),
                        facts,
                        flags);
            }
        }
        var after = cost == null
                ? item.calculation()
                : new CostRecalculation.Calculation(cost.quantity(), cost.unitPrice(), cost.factor(), cost.amount());
        if (!cancelled
                && key == null
                && json.valueToTree(item.calculation()).equals(json.valueToTree(after))
                && json.valueToTree(item.sourceValues()).equals(json.valueToTree(facts))) return;
        boolean reset = Set.of("CHECKED", "APPROVED").contains(item.status());
        if (cancelled)
            store.cancel(
                    item.id(),
                    switch (cause) {
                        case "END_DATE_CHANGED" -> "Đổi ngày kết thúc dự kiến";
                        case "RECORD_DEATH" -> "Ngày sau ngày qua đời";
                        case "ADMISSION_CANCELLED" -> "Hủy tiếp nhận";
                        default -> cause;
                    },
                    time.now());
        else store.recalculate(item.id(), cost);
        history.add(
                CostRecalculation.TRACK,
                item.id(),
                new HistoryEntry<>(
                        HistoryKind.POINT,
                        item.status(),
                        cancelled ? "CANCELLED" : "DRAFT",
                        null,
                        time.now(),
                        "RECALCULATE",
                        null,
                        true,
                        null,
                        null,
                        null,
                        new CostRecalculation(item.calculation(), after, cause, reset),
                        null,
                        null));
        statements.touch(item.statementId(), true);
        audit(
                item,
                "COST_ITEM_RECALCULATE",
                Map.of("calculation", item.calculation()),
                Map.of("calculation", after, "cancelled", cancelled, "cause", cause));
        if (reset)
            notifications.staff(
                    "COST_RECALCULATED",
                    item.statementId(),
                    item.residentId(),
                    "M11.RECALCULATED:" + item.id() + ":" + time.now(),
                    "Nguồn đổi; khoản cần được kiểm tra lại",
                    Role.OFFICE_STAFF,
                    time.now(),
                    null);
        events.publishEvent(new CostItemChanged(item.residentId(), item.periodId()));
    }

    private void audit(
            StoredCostCalculation item, String action, Map<String, Object> before, Map<String, Object> after) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, false))
                .entity("M11.COST_ITEM", item.id())
                .resident(item.residentId())
                .before(before)
                .after(after)
                .build());
    }

    private StoredCostCalculation lockCurrent(StoredCostCalculation selected) {
        locks.resident(selected.residentId());
        statements.lockPeriod(selected.periodId());
        locks.statement(selected.residentId(), selected.statementId());
        return store.lock(selected.id());
    }
}
