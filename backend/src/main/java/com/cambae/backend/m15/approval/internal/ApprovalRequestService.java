package com.cambae.backend.m15.approval.internal;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.AlreadyProcessedException;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.error.ReasonRequiredException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.error.VersionMismatchException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.Target;
import com.cambae.backend.m15.approval.api.ApprovalCommands;
import com.cambae.backend.m15.approval.api.ApprovalRequestApi;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.ApprovalStatus;
import com.cambae.backend.m15.approval.api.CreateRequest;
import com.cambae.backend.m15.approval.api.RequestApplyFailed;
import com.cambae.backend.m15.approval.api.RequestCancelled;
import com.cambae.backend.m15.approval.api.RequestCreated;
import com.cambae.backend.m15.approval.api.RequestDecided;
import com.cambae.backend.m15.approval.api.RequestSubmitted;
import com.cambae.backend.m15.approval.api.RequestTypeHandler;
import com.cambae.backend.m15.approval.api.ResultRef;
import com.cambae.backend.m15.approval.internal.RequestTransitionTable.Command;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;

/**
 * Vòng đời yêu cầu phê duyệt (research R-10; FR-031 → FR-041, BR-M15-08; 000 T092). Mỗi lệnh khóa dòng yêu cầu, kiểm
 * quyền tại lúc này (FR-034a), kiểm trạng thái theo {@link RequestTransitionTable}, kiểm If-Match (FR-040), ghi nhật ký
 * cùng transaction (FR-041) và phát sự kiện. Duyệt khi ngày hiệu lực thực tế đã tới thì áp dụng ngay ở transaction riêng
 * sau khi quyết định commit (FR-037).
 */
@Service
public class ApprovalRequestService implements ApprovalRequestApi {

    /** Số dòng mỗi trang danh sách (thông số kỹ thuật). */
    static final int PAGE_SIZE = 50;

    private static final Set<String> DECIDED = Set.of(
            ApprovalStatus.APPROVED,
            ApprovalStatus.APPLIED,
            ApprovalStatus.APPLY_FAILED,
            ApprovalStatus.REJECTED,
            ApprovalStatus.CANCELLED);

    private final ApprovalRequestStore store;
    private final ApproverDirectory directory;
    private final RequestApplier applier;
    private final RequestTransitionTable table = new RequestTransitionTable();
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<PermissionApi> permissions;

    ApprovalRequestService(
            ApprovalRequestStore store,
            ApproverDirectory directory,
            RequestApplier applier,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<PermissionApi> permissions) {
        this.store = store;
        this.directory = directory;
        this.applier = applier;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.currentUser = currentUser;
        this.permissions = permissions;
    }

    /** Hộp yêu cầu (hợp đồng {@code inbox}). */
    public enum Inbox {
        MINE,
        AWAITING_MY_APPROVAL,
        ALL_IN_SCOPE
    }

    /** Yêu cầu kèm lệnh người xem hiện được làm. */
    public record Viewed(ApprovalRequestView request, List<String> allowedCommands) {}

    public record Page(List<Viewed> items, String nextCursor) {}

    // ---- ApprovalRequestApi -------------------------------------------------------------------------------------

    @Override
    @Transactional
    public ApprovalRequestView create(CreateRequest command) {
        if (command.reason() == null) {
            throw new ReasonRequiredException();
        }
        CurrentUser user = currentUser.getObject();
        RequestTypeHandler handler = directory.handler(command.type());
        handler.validateContent(command.content());
        Target target = handler.scope(command.content());
        if (!permissions
                .getObject()
                .hasPermission(user, handler.createPermission(), target)
                .allowed()) {
            throw new PermissionDeniedException("Không có quyền lập loại yêu cầu " + command.type());
        }
        Instant now = time.now();
        long id = store.insert(
                command.type(),
                command.content(),
                target,
                user.accountId(),
                now,
                command.reason().value(),
                command.requestedEffectiveDate(),
                String.valueOf(time.today().getYear()));
        ApprovalRequestView created = store.find(id).orElseThrow();
        log(ApprovalActions.CREATED, created, null, Map.of("status", ApprovalStatus.DRAFT), command.reason(), null);
        events.publishEvent(new RequestCreated(created));
        if (command.submitImmediately()) {
            return submitLocked(user, created, handler);
        }
        return created;
    }

    @Override
    public Optional<ApprovalRequestView> find(long id) {
        return store.find(id);
    }

    @Override
    public Optional<ResultRef> applicationResult(long requestId) {
        return store.find(requestId).map(ApprovalRequestView::applicationResult);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyExtendedTransition(long requestId, String transitionCode, Basis basis) {
        ApprovalRequestView request = store.lock(requestId).orElseThrow(NotFoundException::new);
        RequestTypeHandler handler = directory.handler(request.type());
        String to = table.nextExtended(request.status(), handler.extendedTransitions(), transitionCode);
        store.update(requestId, "status = ?", to);
        auditLog.write(AuditLogEntry.builder()
                .action(ApprovalActions.EXTENDED_TRANSITION)
                .entity(ApprovalActions.ENTITY, requestId)
                .resident(request.target().residentId())
                .before(Map.of("status", request.status()))
                .after(Map.of("status", to))
                .basis(basis)
                .details(Map.of("transition", transitionCode))
                .build());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void closeForFinalState(long requestId, Basis basis) {
        ApprovalRequestView request = store.lock(requestId).orElseThrow(NotFoundException::new);
        if (table.isFinal(request.status())) {
            return;
        }
        String to = table.next(request.status(), Command.CLOSE_FOR_FINAL_STATE);
        String reason = "Người cao tuổi sang trạng thái cuối";
        store.update(requestId, "status = ?, closing_reason = ?", to, reason);
        auditLog.write(AuditLogEntry.builder()
                .action(ApprovalActions.CLOSED_FOR_FINAL_STATE)
                .entity(ApprovalActions.ENTITY, requestId)
                .resident(request.target().residentId())
                .before(Map.of("status", request.status()))
                .after(Map.of("status", to))
                .basis(basis)
                .build());
        ApprovalRequestView closed = store.find(requestId).orElseThrow();
        if (ApprovalStatus.CANCELLED.equals(to)) {
            events.publishEvent(new RequestCancelled(closed));
        } else {
            events.publishEvent(
                    new RequestApplyFailed(closed, List.of(new UnmetCondition("FR-031", reason)), time.now()));
        }
    }

    // ---- Lệnh qua HTTP ------------------------------------------------------------------------------------------

    @BusinessCommand(requiresReason = true)
    @Transactional
    public ApprovalRequestView editContent(
            long id, String ifMatch, JsonNode content, LocalDate requestedEffectiveDate, Reason reason) {
        CurrentUser user = currentUser.getObject();
        ApprovalRequestView request = locked(id);
        RequestTypeHandler handler = directory.handler(request.type());
        requireRequester(user, request);
        table.next(request.status(), Command.EDIT_CONTENT);
        requireVersion(request, ifMatch);
        handler.validateContent(content);
        Target target = handler.scope(content);
        LocalDate date = requestedEffectiveDate == null ? request.requestedEffectiveDate() : requestedEffectiveDate;
        store.update(
                id,
                "content = ?, requested_effective_date = ?, entity_type = ?, entity_id = ?, resident_id = ?,"
                        + " floor_area_id = ?",
                store.toJson(content),
                date,
                target.type(),
                target.id(),
                target.residentId(),
                target.floorAreaId());
        Map<String, Object> before = new HashMap<>();
        before.put("content", request.content());
        before.put("requestedEffectiveDate", request.requestedEffectiveDate().toString());
        Map<String, Object> after = new HashMap<>();
        after.put("content", content);
        after.put("requestedEffectiveDate", date.toString());
        log(ApprovalActions.DRAFT_EDITED, request, before, after, reason, null);
        return store.find(id).orElseThrow();
    }

    @Transactional
    public ApprovalRequestView submit(long id, String ifMatch) {
        CurrentUser user = currentUser.getObject();
        ApprovalRequestView request = locked(id);
        RequestTypeHandler handler = directory.handler(request.type());
        requireRequester(user, request);
        table.next(request.status(), Command.SUBMIT);
        requireVersion(request, ifMatch);
        return submitLocked(user, request, handler);
    }

    @Transactional
    public ApprovalRequestView approve(long id, String ifMatch, String comment) {
        CurrentUser user = currentUser.getObject();
        ApprovalRequestView request = locked(id);
        RequestTypeHandler handler = directory.handler(request.type());
        requireDecider(user, request, handler);
        requirePending(request, Command.APPROVE);
        requireVersion(request, ifMatch);
        boolean self = request.requesterId() == user.accountId();
        if (self && !handler.allowSelfApproval()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "SELF_APPROVAL_NOT_ALLOWED",
                    "Loại yêu cầu này không cho người yêu cầu tự duyệt");
        }
        Reason opinion = comment == null || comment.isBlank() ? null : new Reason(comment);
        if (self && opinion == null) {
            throw new ReasonRequiredException();
        }
        List<UnmetCondition> unmet = handler.checkOnApprove(request);
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        LocalDate today = time.today();
        LocalDate actual = request.requestedEffectiveDate().isAfter(today) ? request.requestedEffectiveDate() : today;
        Instant now = time.now();
        store.update(
                id,
                "status = ?, actual_effective_date = ?, decided_by = ?, decided_at = ?, decision_comment = ?,"
                        + " self_approved = ?",
                table.next(request.status(), Command.APPROVE),
                actual,
                user.accountId(),
                ApprovalRequestStore.utc(now),
                opinion == null ? null : opinion.value(),
                self);
        ApprovalRequestView decided = store.find(id).orElseThrow();
        handler.onApprove(decided, actual);
        log(
                ApprovalActions.APPROVED,
                request,
                Map.of("status", request.status()),
                Map.of("status", ApprovalStatus.APPROVED, "actualEffectiveDate", actual.toString()),
                opinion,
                self ? Map.of("selfApproved", true) : null);
        events.publishEvent(new RequestDecided(decided, ApprovalStatus.APPROVED));
        if (!actual.isAfter(today)) {
            // Áp dụng ngay ở transaction riêng sau khi quyết định đã commit; lỗi thì giữ Đã duyệt (FR-037, FR-037b)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    applier.applySafely(id, null);
                }
            });
        }
        return decided;
    }

    @BusinessCommand(requiresReason = true)
    @Transactional
    public ApprovalRequestView reject(long id, String ifMatch, Reason comment) {
        CurrentUser user = currentUser.getObject();
        ApprovalRequestView request = locked(id);
        RequestTypeHandler handler = directory.handler(request.type());
        requireDecider(user, request, handler);
        requirePending(request, Command.REJECT);
        requireVersion(request, ifMatch);
        store.update(
                id,
                "status = ?, decided_by = ?, decided_at = ?, decision_comment = ?, self_approved = ?",
                table.next(request.status(), Command.REJECT),
                user.accountId(),
                ApprovalRequestStore.utc(time.now()),
                comment.value(),
                request.requesterId() == user.accountId());
        log(
                ApprovalActions.REJECTED,
                request,
                Map.of("status", request.status()),
                Map.of("status", ApprovalStatus.REJECTED),
                comment,
                null);
        ApprovalRequestView rejected = store.find(id).orElseThrow();
        events.publishEvent(new RequestDecided(rejected, ApprovalStatus.REJECTED));
        return rejected;
    }

    /** Hủy: Nháp, Chờ duyệt bởi người yêu cầu; Đã duyệt bởi người có quyền duyệt (data-model mục 3; BR-M15-08). */
    @BusinessCommand(requiresReason = true)
    @Transactional
    public ApprovalRequestView cancel(long id, String ifMatch, Reason reason) {
        CurrentUser user = currentUser.getObject();
        ApprovalRequestView request = locked(id);
        RequestTypeHandler handler = directory.handler(request.type());
        if (ApprovalStatus.APPROVED.equals(request.status())) {
            requireDecider(user, request, handler);
        } else if (!table.isFinal(request.status())) {
            requireRequester(user, request);
        }
        String to = table.next(request.status(), Command.CANCEL);
        requireVersion(request, ifMatch);
        store.update(id, "status = ?, closing_reason = ?", to, reason.value());
        log(
                ApprovalActions.CANCELLED,
                request,
                Map.of("status", request.status()),
                Map.of("status", ApprovalStatus.CANCELLED),
                reason,
                null);
        ApprovalRequestView cancelled = store.find(id).orElseThrow();
        events.publishEvent(new RequestCancelled(cancelled));
        return cancelled;
    }

    // ---- Truy vấn qua HTTP --------------------------------------------------------------------------------------

    /** Chi tiết; người không xem được thì 404, không lộ tồn tại. */
    @Transactional(readOnly = true)
    public Viewed detail(long id) {
        CurrentUser user = currentUser.getObject();
        ApprovalRequestView request = store.find(id).orElseThrow(NotFoundException::new);
        RequestTypeHandler handler = directory.handler(request.type());
        if (!directory.canView(user, request, handler)) {
            throw new NotFoundException();
        }
        return new Viewed(request, allowedCommands(user, request, handler));
    }

    /** Danh sách theo hộp, mới nhất trước; quyền xét từng dòng nên quét theo lô cho tới khi đủ trang. */
    @Transactional(readOnly = true)
    public Page list(Inbox inbox, List<String> statuses, String type, String cursor) {
        CurrentUser user = currentUser.getObject();
        Inbox box = inbox == null ? Inbox.MINE : inbox;
        List<String> where = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (box == Inbox.MINE) {
            where.add("r.requester_id = ?");
            args.add(user.accountId());
        }
        if (box == Inbox.AWAITING_MY_APPROVAL) {
            where.add("r.status = ?");
            args.add(ApprovalStatus.PENDING_APPROVAL);
        }
        if (statuses != null && !statuses.isEmpty()) {
            where.add("r.status IN (" + String.join(",", Collections.nCopies(statuses.size(), "?")) + ")");
            args.addAll(statuses);
        }
        if (type != null) {
            where.add("r.type = ?");
            args.add(type);
        }
        long before = cursor == null ? Long.MAX_VALUE : parseCursor(cursor);
        List<Viewed> items = new ArrayList<>();
        while (items.size() < PAGE_SIZE) {
            List<Object> batchArgs = new ArrayList<>(args);
            batchArgs.add(before);
            List<ApprovalRequestView> batch = store.query(
                    (where.isEmpty() ? "" : String.join(" AND ", where) + " AND ") + "r.id < ? ORDER BY r.id DESC"
                            + " LIMIT " + (PAGE_SIZE * 4),
                    batchArgs.toArray());
            for (ApprovalRequestView request : batch) {
                before = request.id();
                RequestTypeHandler handler = directory.handler(request.type());
                boolean shown = switch (box) {
                    case MINE -> true;
                    case AWAITING_MY_APPROVAL -> directory.canDecide(user, request, handler);
                    case ALL_IN_SCOPE -> directory.canView(user, request, handler);
                };
                if (shown) {
                    items.add(new Viewed(request, allowedCommands(user, request, handler)));
                    if (items.size() == PAGE_SIZE) {
                        return new Page(items, String.valueOf(request.id()));
                    }
                }
            }
            if (batch.size() < PAGE_SIZE * 4) {
                break;
            }
        }
        return new Page(items, null);
    }

    /** Lệnh người xem hiện được làm, cùng điều kiện quyền, phạm vi, giới hạn vai trò với chính lệnh (FR-034). */
    List<String> allowedCommands(CurrentUser user, ApprovalRequestView request, RequestTypeHandler handler) {
        boolean requester = request.requesterId() == user.accountId();
        List<String> commands = new ArrayList<>();
        switch (request.status()) {
            case ApprovalStatus.DRAFT -> {
                if (requester) {
                    commands.addAll(
                            List.of(ApprovalCommands.EDIT_CONTENT, ApprovalCommands.SUBMIT, ApprovalCommands.CANCEL));
                }
            }
            case ApprovalStatus.PENDING_APPROVAL -> {
                if (directory.canDecide(user, request, handler) && (!requester || handler.allowSelfApproval())) {
                    commands.addAll(List.of(ApprovalCommands.APPROVE, ApprovalCommands.REJECT));
                }
                if (requester) {
                    commands.add(ApprovalCommands.CANCEL);
                }
            }
            case ApprovalStatus.APPROVED -> {
                if (directory.canDecide(user, request, handler)) {
                    commands.add(ApprovalCommands.CANCEL);
                }
            }
            default -> {}
        }
        return commands;
    }

    // ---- Phần chung ---------------------------------------------------------------------------------------------

    private ApprovalRequestView submitLocked(
            CurrentUser user, ApprovalRequestView request, RequestTypeHandler handler) {
        // FR-034a: người yêu cầu mất quyền tạo thì Nháp không gửi duyệt được nữa
        if (!permissions
                .getObject()
                .hasPermission(user, handler.createPermission(), request.target())
                .allowed()) {
            throw new PermissionDeniedException("Không còn quyền lập loại yêu cầu " + request.type());
        }
        Instant now = time.now();
        store.update(
                request.id(),
                "status = ?, submitted_at = ?",
                table.next(request.status(), Command.SUBMIT),
                ApprovalRequestStore.utc(now));
        log(
                ApprovalActions.SUBMITTED,
                request,
                Map.of("status", request.status()),
                Map.of("status", ApprovalStatus.PENDING_APPROVAL),
                null,
                null);
        ApprovalRequestView submitted = store.find(request.id()).orElseThrow();
        events.publishEvent(new RequestSubmitted(submitted, directory.approvers(submitted, handler), now));
        return submitted;
    }

    private ApprovalRequestView locked(long id) {
        return store.lock(id).orElseThrow(NotFoundException::new);
    }

    private static void requireRequester(CurrentUser user, ApprovalRequestView request) {
        if (request.requesterId() != user.accountId()) {
            throw new PermissionDeniedException("Chỉ người yêu cầu được thực hiện lệnh này");
        }
    }

    private void requireDecider(CurrentUser user, ApprovalRequestView request, RequestTypeHandler handler) {
        if (!directory.canDecide(user, request, handler)) {
            throw new PermissionDeniedException("Không có quyền duyệt yêu cầu này (FR-034)");
        }
    }

    /** Quyết định đến sau trên yêu cầu đã rời Chờ duyệt: 409 {@code ALREADY_PROCESSED} (FR-040). */
    private void requirePending(ApprovalRequestView request, Command command) {
        if (DECIDED.contains(request.status())) {
            throw new AlreadyProcessedException("Yêu cầu " + request.displayCode() + " đã được xử lý");
        }
        table.next(request.status(), command);
    }

    private static void requireVersion(ApprovalRequestView request, String ifMatch) {
        String value = ifMatch == null ? "" : ifMatch.strip();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        if (!value.replace("\"", "").equals(String.valueOf(request.version()))) {
            throw new VersionMismatchException(request);
        }
    }

    private static long parseCursor(String cursor) {
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            throw new InvalidValueException("Con trỏ trang không hợp lệ");
        }
    }

    private void log(
            AuditAction action,
            ApprovalRequestView request,
            Map<String, Object> before,
            Map<String, Object> after,
            Reason reason,
            Map<String, Object> details) {
        auditLog.write(AuditLogEntry.builder()
                .action(action)
                .entity(ApprovalActions.ENTITY, request.id())
                .resident(request.target().residentId())
                .before(before)
                .after(after)
                .reason(reason)
                .details(details)
                .build());
    }
}
