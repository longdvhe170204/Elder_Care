package com.eldercare.backend.billing.internal.period.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.billing.api.events.*;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.item.domain.CostItemRecord;
import com.eldercare.backend.billing.internal.item.persistence.CostItemStore;
import com.eldercare.backend.billing.internal.period.domain.*;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T017/P-07: chốt khoản, tổng bảng, sự kiện và nhật ký trong cùng transaction. */
@Service
@Transactional
public class CostStatementService {
    private final CostStatementStore store;
    private final CostItemStore items;
    private final CostLock locks;
    private final CostAccess access;
    private final CostBillingCoverage coverage;
    private final ParameterApi parameters;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final CostNotifications notifications;
    private final com.eldercare.backend.billing.internal.item.service.CostItemHistory itemHistory;

    public CostStatementService(
            CostStatementStore store,
            CostItemStore items,
            CostLock locks,
            CostAccess access,
            CostBillingCoverage coverage,
            ParameterApi parameters,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            CostNotifications notifications,
            com.eldercare.backend.billing.internal.item.service.CostItemHistory itemHistory) {
        this.store = store;
        this.items = items;
        this.locks = locks;
        this.access = access;
        this.coverage = coverage;
        this.parameters = parameters;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.notifications = notifications;
        this.itemHistory = itemHistory;
    }

    public CostStatementRecord read(long id) {
        var s = store.statement(id);
        access.viewOrCommand(Function.COST_ADJUSTMENT, Operation.VIEW, "M11.COST_STATEMENT", id, s.residentId());
        return s;
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.PERIOD_CLOSING, operation = Operation.UPDATE)
    public Optional<PeriodCostChange.Warning> submit(long id) {
        var s = locked(id, Role.OFFICE_STAFF, Operation.UPDATE);
        if (!s.status().equals("OPEN"))
            throw invalid("CONDITION_NOT_MET", "Bảng không ở trạng thái đang mở", List.of());
        if (s.statementType().equals("REGULAR")
                && !s.finalPeriod()
                && !time.today().isAfter(s.month().atEndOfMonth()))
            throw invalid("PERIOD_NOT_ENDED", "Chưa qua ngày cuối của bảng", List.of());
        var drafts = items.lockItems(id).stream()
                .filter(i -> i.status().equals("DRAFT"))
                .map(i -> i.id())
                .toList();
        if (!drafts.isEmpty()) throw invalid("DRAFT_ITEMS_REMAIN", "Bảng còn khoản nháp", drafts);
        var warning = warning(s);
        store.submit(id, access.user().accountId(), time.now(), warning.orElse(null));
        log("COST_STATEMENT_SUBMITTED", s, "AWAITING_FINALIZATION", null);
        notifications.staff(
                "COST_STATEMENT_SUBMITTED",
                id,
                s.residentId(),
                "M11:SUBMIT:" + id + ":" + (s.version() + 1),
                "Bảng chi phí đã được gửi chốt",
                Role.NURSING_HOME_MANAGER,
                time.now(),
                access.user().accountId());
        if (warning.isPresent())
            notifications.staff(
                    "COST_CHANGE_WARNING",
                    id,
                    s.residentId(),
                    "M11:CHANGE:" + id + ":" + (s.version() + 1),
                    (warning.get().increaseRatio() == null
                                    ? "Chi phí tăng từ tổng kỳ trước bằng 0 đồng. Các loại tăng nhiều nhất: "
                                    : "Chi phí tăng " + warning.get().increaseRatio()
                                            + "% so với kỳ trước. Các loại tăng nhiều nhất: ")
                            + String.join(
                                    ", ",
                                    warning.get().topThreeIncreases().stream()
                                            .map(this::costTypeLabel)
                                            .toList()),
                    Role.OFFICE_STAFF,
                    time.now(),
                    access.user().accountId());
        return warning;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.PERIOD_CLOSING, operation = Operation.APPROVE)
    public void returnStatement(long id, List<Long> itemIds, Reason reason) {
        var s = locked(id, Role.NURSING_HOME_MANAGER, Operation.APPROVE);
        if (!s.status().equals("AWAITING_FINALIZATION"))
            throw invalid("CONDITION_NOT_MET", "Chỉ trả lại bảng chờ chốt", List.of());
        var selected = itemIds == null
                ? List.<Long>of()
                : itemIds.stream().distinct().sorted().toList();
        var all = items.lockItems(id);
        if (selected.stream()
                .anyMatch(itemId -> all.stream()
                        .noneMatch(i -> i.id() == itemId
                                && Set.of("CHECKED", "APPROVED").contains(i.status()))))
            throw invalid(
                    "CONDITION_NOT_MET", "Khoản được chọn phải thuộc bảng và đã kiểm tra hoặc đã duyệt", selected);
        for (var item : all)
            if (selected.contains(item.id())) {
                items.transition(item, "DRAFT", access.user().accountId(), time.now(), reason.value());
                itemHistory.changed(item, "DRAFT", "RETURN", reason);
                audit.write(AuditLogEntry.builder()
                        .action(new AuditAction("COST_ITEM_RETURNED", true))
                        .entity("M11.COST_ITEM", item.id())
                        .resident(item.residentId())
                        .before(Map.of("status", item.status()))
                        .after(Map.of("status", "DRAFT"))
                        .reason(reason)
                        .build());
            }
        store.touch(id, true);
        log("COST_STATEMENT_RETURNED", s, "OPEN", reason);
        events.publishEvent(new CostItemChanged(s.residentId(), s.periodId()));
        notifications.staff(
                "COST_STATEMENT_RETURNED",
                id,
                s.residentId(),
                "M11:RETURN_TABLE:" + id + ":" + (s.version() + 1),
                "Bảng chi phí được trả lại để đối chiếu",
                Role.OFFICE_STAFF,
                time.now(),
                access.user().accountId());
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.PERIOD_CLOSING, operation = Operation.FINALIZE)
    public void finalizeStatement(long id, String ifMatch) {
        var s = locked(id, Role.NURSING_HOME_MANAGER, Operation.FINALIZE);
        VersionEtag.requireMatch(ifMatch, s.version(), Map.of("id", id, "status", s.status(), "version", s.version()));
        if (!s.status().equals("AWAITING_FINALIZATION"))
            throw invalid("CONDITION_NOT_MET", "Bảng chưa được gửi chốt", List.of());
        var all = items.lockItems(id);
        var blocked = all.stream()
                .filter(i -> !Set.of("APPROVED", "CANCELLED").contains(i.status()))
                .map(i -> i.id())
                .toList();
        if (!blocked.isEmpty()) throw invalid("UNAPPROVED_ITEMS_REMAIN", "Bảng còn khoản chưa duyệt", blocked);
        var at = time.now();
        var totals = totals(all, false);
        for (var item : all)
            if (item.status().equals("APPROVED")) {
                items.transition(item, "FINALIZED", access.user().accountId(), at, null);
                itemHistory.changed(item, "FINALIZED", "FINALIZE", null);
                audit.write(AuditLogEntry.builder()
                        .action(new AuditAction("COST_ITEM_FINALIZED", false))
                        .entity("M11.COST_ITEM", item.id())
                        .resident(s.residentId())
                        .before(Map.of("status", "APPROVED"))
                        .after(Map.of("status", "FINALIZED"))
                        .build());
            }
        store.finalizeStatement(id, access.user().accountId(), at, totals);
        if (store.finalizePeriodIfComplete(s.periodId(), coverage.expected(s.month()), at))
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("COST_PERIOD_FINALIZED", false))
                    .entity("M11.COST_PERIOD", s.periodId())
                    .before(Map.of("status", "AWAITING_FINALIZATION"))
                    .after(Map.of("status", "FINALIZED"))
                    .build());
        log("COST_STATEMENT_FINALIZED", s, "FINALIZED", null);
        var request = notifications.request(
                "COST_STATEMENT_FINALIZED",
                "M11.COST_STATEMENT",
                id,
                s.residentId(),
                "M11:FINALIZED:" + id,
                "Bảng chi phí đã được chốt",
                List.of(new RecipientGroup.FamilyWithRight(s.residentId(), FamilyRight.VIEW_COSTS)),
                NotificationLevel.MEDIUM,
                at,
                access.user().accountId());
        events.publishEvent(new CostStatementFinalized(
                id,
                CostStatementType.valueOf(s.statementType()),
                s.periodId(),
                s.residentId(),
                totals.values().stream().reduce(0L, Math::addExact),
                at,
                List.of(request)));
        events.publishEvent(new CostItemChanged(s.residentId(), s.periodId()));
    }

    private CostStatementRecord locked(long id, Role role, Operation operation) {
        access.require(role, Function.PERIOD_CLOSING, operation, "M11.COST_STATEMENT", 0, null);
        var s = store.statement(id);
        access.require(role, Function.PERIOD_CLOSING, operation, "M11.COST_STATEMENT", id, s.residentId());
        locks.resident(s.residentId());
        store.lockPeriod(s.periodId());
        locks.statement(s.residentId(), id);
        s = store.lockedStatement(id);
        if (s.status().equals("FINALIZED")) throw invalid("STATEMENT_FINALIZED", "Bảng đã chốt", List.of());
        return s;
    }

    private Optional<PeriodCostChange.Warning> warning(CostStatementRecord s) {
        if (!s.statementType().equals("REGULAR")) return Optional.empty();
        return store.previous(s)
                .flatMap(p -> PeriodCostChange.compare(
                        totals(items.lockItems(s.id()), true),
                        totals(items.lockItems(p.id()), true),
                        coverage.fullMonth(s.residentId(), s.month()) && coverage.fullMonth(p.residentId(), p.month()),
                        parameters.read(ParameterCode.CFG_M11_03).asPercent()));
    }

    private void log(String action, CostStatementRecord s, String status, Reason reason) {
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", status);
        if (status.equals("FINALIZED")) {
            var totals = totals(items.lockItems(s.id()), false);
            after.put("total", totals.values().stream().reduce(0L, Math::addExact));
            after.put("totalsByType", totals);
        }
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, reason != null))
                .entity("M11.COST_STATEMENT", s.id())
                .resident(s.residentId())
                .before(Map.of("status", s.status()))
                .after(after)
                .reason(reason)
                .build());
    }

    private Map<CostType, Long> totals(List<CostItemRecord> rows, boolean comparison) {
        var totals = new EnumMap<CostType, Long>(CostType.class);
        for (var item : rows)
            if (!item.status().equals("CANCELLED")
                    && item.amount() != null
                    && (!comparison
                            || !item.origin().equals("ADJUSTMENT")
                            || Objects.equals(item.originalPeriodId(), item.periodId())))
                totals.merge(item.costType(), item.amount(), Math::addExact);
        return totals;
    }

    private BusinessException invalid(String code, String message, List<Long> itemIds) {
        return new BusinessException(
                HttpStatus.CONFLICT,
                code,
                message,
                Map.of(
                        "unmetConditions",
                        itemIds.stream()
                                .map(id -> Map.of("code", code, "itemId", id))
                                .toList()));
    }

    private String costTypeLabel(String type) {
        return switch (CostType.valueOf(type)) {
            case RESIDENCY_FEE -> "Phí lưu trú";
            case DAY_CARE_SESSION_FEE -> "Phí buổi bán trú";
            case SERVICE -> "Dịch vụ";
            case CONSUMABLE -> "Vật phẩm tiêu hao";
            case ACTIVITY -> "Hoạt động";
            case MEDICATION -> "Thuốc";
            case FAMILY_OVERNIGHT_STAY -> "Người thân ở lại";
            case FAMILY_MEAL -> "Suất ăn người thân";
            case PURCHASE_ASSISTANCE -> "Mua hộ";
            case OTHER -> "Khoản khác";
        };
    }
}
