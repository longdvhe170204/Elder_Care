package com.eldercare.backend.billing.internal.item.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.billing.api.events.CostItemChanged;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.item.domain.*;
import com.eldercare.backend.billing.internal.item.persistence.CostItemStore;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.period.service.*;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T017: mọi khoản của lô dùng cùng transaction; khóa người, kỳ rồi bảng theo id tăng. */
@Service
@Transactional
public class CostItemService {
    private final CostItemStore store;
    private final CostStatementStore statements;
    private final CostLock locks;
    private final CostAccess access;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final CostNotifications notifications;
    private final CostItemHistory history;

    public CostItemService(
            CostItemStore store,
            CostStatementStore statements,
            CostLock locks,
            CostAccess access,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            CostNotifications notifications,
            CostItemHistory history) {
        this.store = store;
        this.statements = statements;
        this.locks = locks;
        this.access = access;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.notifications = notifications;
        this.history = history;
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.COST_ADJUSTMENT, operation = Operation.UPDATE)
    public CostBatchResult check(CostItemSelection selection) {
        return batch(selection, false, null);
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.COST_ADJUSTMENT, operation = Operation.APPROVE)
    public CostBatchResult approve(CostItemSelection selection, String excessReason) {
        return batch(selection, true, excessReason);
    }

    private CostBatchResult batch(CostItemSelection selection, boolean approving, String excessReason) {
        var role = approving ? Role.NURSING_HOME_MANAGER : Role.OFFICE_STAFF;
        var operation = approving ? Operation.APPROVE : Operation.UPDATE;
        access.require(role, Function.COST_ADJUSTMENT, operation, "M11.COST_ITEM", 0, null);
        if (selection.filter() != null) {
            var s = statements.statement(selection.filter().statementId());
            access.require(role, Function.COST_ADJUSTMENT, operation, "M11.COST_STATEMENT", s.id(), s.residentId());
            locks.resident(s.residentId());
            statements.lockPeriod(s.periodId());
            locks.statement(s.residentId(), s.id());
        }
        var selected = store.selected(selection);
        selected.stream().map(CostItemRecord::residentId).distinct().sorted().forEach(locks::resident);
        selected.stream().map(CostItemRecord::periodId).distinct().sorted().forEach(statements::lockPeriod);
        selected.stream()
                .sorted(Comparator.comparingLong(CostItemRecord::statementId))
                .map(i -> Map.entry(i.statementId(), i.residentId()))
                .distinct()
                .forEach(e -> locks.statement(e.getValue(), e.getKey()));
        // Kiểm quyền cả lô trước khi ghi bất kỳ khoản nào.
        selected.forEach(i ->
                access.require(role, Function.COST_ADJUSTMENT, operation, "M11.COST_ITEM", i.id(), i.residentId()));
        var succeeded = new ArrayList<Long>();
        var skipped = new ArrayList<CostBatchResult.Skipped>();
        for (var item : store.selectedUnderLock(selection)) {
            var statement = statements.lockedStatement(item.statementId());
            String code = null, message = null;
            if (statement.status().equals("FINALIZED")) {
                code = "STATEMENT_FINALIZED";
                message = "Bảng đã chốt";
            } else if (!item.status().equals(approving ? "CHECKED" : "DRAFT")) {
                code = "CONDITION_NOT_MET";
                message = "Trạng thái khoản không phù hợp với lệnh";
            } else if (item.amount() == null || item.flags().contains("MISSING_UNIT_PRICE")) {
                code = "CONDITION_NOT_MET";
                message = "Khoản chưa có đơn giá, số tiền";
            } else if (approving && !selection.individual() && !item.origin().equals("EVENT_BASED")) {
                code = "CONDITION_NOT_MET";
                message = "Khoản nhập tay, mua hộ, điều chỉnh phải được duyệt riêng";
            } else if (approving
                    && Objects.equals(item.checkedBy(), access.user().accountId())) {
                code = "PERMISSION_DENIED";
                message = "Người kiểm tra không được tự duyệt";
            } else if (approving
                    && exceeds(item)
                    && (excessReason == null || excessReason.isBlank() || excessReason.length() > Reason.MAX_LENGTH)) {
                code = "EXCESS_APPROVAL_REASON_REQUIRED";
                message = "Cần lý do chấp nhận phần vượt";
            }
            if (code != null) {
                skipped.add(new CostBatchResult.Skipped(item.id(), code, message));
                continue;
            }
            change(
                    item,
                    approving ? "APPROVED" : "CHECKED",
                    approving && exceeds(item) ? new Reason(excessReason) : null);
            if (approving && exceeds(item))
                events.publishEvent(new com.eldercare.backend.billing.internal.period.domain.CostNotificationEvent(
                        List.of(notifications.request(
                                "PURCHASE_EXCESS_APPROVED",
                                "M11.COST_ITEM",
                                item.id(),
                                item.residentId(),
                                "M11:EXCESS:" + item.id() + ":"
                                        + statements
                                                .statement(item.statementId())
                                                .version(),
                                "Phần vượt số đã đồng ý của khoản mua hộ đã được duyệt. Số được phép: "
                                        + Objects.toString(store.purchaseAllowedAmount(item.id()), "chưa có căn cứ")
                                        + "; số thực tế: " + item.amount() + " đồng. Lý do: " + excessReason,
                                List.of(new RecipientGroup.Representative(item.residentId())),
                                NotificationLevel.LOW,
                                time.now(),
                                access.user().accountId()))));
            succeeded.add(item.id());
        }
        return new CostBatchResult(succeeded, skipped);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.COST_ADJUSTMENT, operation = Operation.UPDATE)
    public void uncheck(long id, Reason reason) {
        var item = locked(id, Role.OFFICE_STAFF, Operation.UPDATE);
        if (!item.status().equals("CHECKED")
                || !statements.lockedStatement(item.statementId()).status().equals("OPEN"))
            throw invalid("CONDITION_NOT_MET", "Chỉ bỏ kiểm tra khi khoản đã kiểm tra và bảng đang mở");
        change(item, "DRAFT", reason, "UNCHECK");
    }

    @BusinessCommand
    @RequiresPermission(function = Function.COST_ADJUSTMENT, operation = Operation.APPROVE)
    public void returnItem(long id, Reason reason) {
        var item = locked(id, Role.NURSING_HOME_MANAGER, Operation.APPROVE);
        if (!Set.of("CHECKED", "APPROVED").contains(item.status()))
            throw invalid("CONDITION_NOT_MET", "Chỉ trả lại khoản đã kiểm tra hoặc đã duyệt");
        change(item, "DRAFT", reason, "RETURN");
        notifications.staff(
                "COST_ITEM_RETURNED",
                item.statementId(),
                item.residentId(),
                "M11:RETURN:" + id + ":"
                        + statements.statement(item.statementId()).version(),
                "Khoản chi phí được trả lại để đối chiếu",
                Role.OFFICE_STAFF,
                time.now(),
                access.user().accountId());
    }

    @BusinessCommand
    @RequiresPermission(function = Function.COST_ADJUSTMENT, operation = Operation.UPDATE)
    public void cancel(long id, Reason reason) {
        var item = locked(id, Role.OFFICE_STAFF, Operation.UPDATE);
        if (!item.status().equals("DRAFT") || item.createdBy() != access.user().accountId())
            throw invalid("CONDITION_NOT_MET", "Chỉ hủy khoản nháp do chính mình lập");
        change(item, "CANCELLED", reason);
    }

    private CostItemRecord locked(long id, Role role, Operation operation) {
        access.require(role, Function.COST_ADJUSTMENT, operation, "M11.COST_ITEM", 0, null);
        var item = store.item(id);
        access.require(role, Function.COST_ADJUSTMENT, operation, "M11.COST_ITEM", id, item.residentId());
        locks.resident(item.residentId());
        statements.lockPeriod(item.periodId());
        locks.statement(item.residentId(), item.statementId());
        item = store.lockedItem(id);
        if (statements.lockedStatement(item.statementId()).status().equals("FINALIZED"))
            throw invalid("STATEMENT_FINALIZED", "Bảng đã chốt");
        return item;
    }

    private void change(CostItemRecord item, String status, Reason reason) {
        String operation = switch (status) {
            case "CHECKED" -> "CHECK";
            case "APPROVED" -> "APPROVE";
            case "CANCELLED" -> "CANCEL";
            default -> throw new IllegalArgumentException(status);
        };
        change(item, status, reason, operation);
    }

    private void change(CostItemRecord item, String status, Reason reason, String operation) {
        store.transition(item, status, access.user().accountId(), time.now(), reason == null ? null : reason.value());
        statements.touch(item.statementId(), status.equals("DRAFT"));
        history.changed(item, status, operation, reason);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("COST_ITEM_" + operation, reason != null))
                .entity("M11.COST_ITEM", item.id())
                .resident(item.residentId())
                .before(Map.of("status", item.status()))
                .after(Map.of("status", status))
                .reason(reason)
                .build());
        events.publishEvent(new CostItemChanged(item.residentId(), item.periodId()));
    }

    private boolean exceeds(CostItemRecord item) {
        return item.flags().contains("EXCEEDS_CONSENT") || item.flags().contains("URGENT_EXCESS");
    }

    private static BusinessException invalid(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
