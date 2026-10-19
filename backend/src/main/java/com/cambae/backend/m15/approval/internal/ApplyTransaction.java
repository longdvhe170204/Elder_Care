package com.cambae.backend.m15.approval.internal;

import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.ApprovalStatus;
import com.cambae.backend.m15.approval.api.RequestApplied;
import com.cambae.backend.m15.approval.api.RequestApplyFailed;
import com.cambae.backend.m15.approval.api.RequestTypeHandler;
import com.cambae.backend.m15.approval.api.ResultRef;
import com.cambae.backend.m15.approval.internal.RequestTransitionTable.Command;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Một lần áp dụng yêu cầu Đã duyệt, trong transaction riêng (research R-10; FR-037, FR-037a, FR-038, FR-039): khóa dòng,
 * kiểm còn Đã duyệt và đã đến ngày, kiểm điều kiện, gọi handler, thêm {@code application_result} (khóa chính chặn áp
 * dụng hai lần), chuyển Đã áp dụng, ghi nhật ký. Handler ném lỗi thì cả transaction rollback.
 */
@Component
class ApplyTransaction {

    enum Outcome {
        APPLIED,
        APPLY_FAILED,
        NOT_APPLICABLE
    }

    private final ApprovalRequestStore store;
    private final ApproverDirectory directory;
    private final RequestTransitionTable table = new RequestTransitionTable();
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    ApplyTransaction(
            ApprovalRequestStore store,
            ApproverDirectory directory,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.store = store;
        this.directory = directory;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Outcome apply(long id, Long schedulerRunId) {
        ApprovalRequestView request = store.lock(id).orElse(null);
        LocalDate today = time.today();
        if (request == null
                || !ApprovalStatus.APPROVED.equals(request.status())
                || request.actualEffectiveDate().isAfter(today)) {
            return Outcome.NOT_APPLICABLE;
        }
        RequestTypeHandler handler = directory.handler(request.type());
        Instant now = time.now();
        Basis basis = new Basis(null, id, null, schedulerRunId);
        List<UnmetCondition> unmet = handler.checkApplyConditions(request);
        if (!unmet.isEmpty()) {
            String reasons =
                    unmet.stream().map(c -> c.code() + ": " + c.description()).collect(Collectors.joining("; "));
            store.update(
                    id, "status = ?, closing_reason = ?", table.next(request.status(), Command.APPLY_FAILED), reasons);
            auditLog.write(AuditLogEntry.builder()
                    .action(ApprovalActions.APPLY_FAILED)
                    .entity(ApprovalActions.ENTITY, id)
                    .resident(request.target().residentId())
                    .before(Map.of("status", request.status()))
                    .after(Map.of("status", ApprovalStatus.APPLY_FAILED))
                    .basis(basis)
                    .details(Map.of("unmetConditions", reasons))
                    .build());
            events.publishEvent(new RequestApplyFailed(store.find(id).orElseThrow(), unmet, now));
            return Outcome.APPLY_FAILED;
        }
        LocalDate effective = request.actualEffectiveDate();
        ResultRef result = handler.apply(request, effective);
        store.insertResult(id, result, now);
        boolean catchUp = effective.isBefore(today);
        store.update(
                id,
                "status = ?, applied_at = ?, catch_up_applied = ?",
                table.next(request.status(), Command.APPLY),
                ApprovalRequestStore.utc(now),
                catchUp);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("actualEffectiveDate", effective.toString());
        details.put("appliedAt", now.toString());
        details.put("catchUpApplied", catchUp);
        details.put("result", Map.of("type", result.type(), "id", result.id()));
        auditLog.write(AuditLogEntry.builder()
                .action(ApprovalActions.APPLIED)
                .entity(ApprovalActions.ENTITY, id)
                .resident(request.target().residentId())
                .before(Map.of("status", request.status()))
                .after(Map.of("status", ApprovalStatus.APPLIED))
                .basis(basis)
                .details(details)
                .build());
        events.publishEvent(new RequestApplied(store.find(id).orElseThrow(), result));
        return Outcome.APPLIED;
    }
}
