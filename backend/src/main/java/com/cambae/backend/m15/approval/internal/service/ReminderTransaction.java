package com.cambae.backend.m15.approval.internal.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.ApprovalStatus;
import com.cambae.backend.m15.approval.api.ApproverReminderDue;
import com.cambae.backend.m15.approval.api.ManagerEscalationDue;
import com.cambae.backend.m15.approval.api.OverdueReminder;
import com.cambae.backend.m15.approval.api.OverdueReminder.Decision;
import com.cambae.backend.m15.approval.api.OverdueReminder.EscalationReason;
import com.cambae.backend.m15.approval.api.spi.RequestTypeHandler;
import com.cambae.backend.m15.approval.internal.domain.ApprovalActions;
import com.cambae.backend.m15.approval.internal.persistence.ApprovalRequestStore;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xét một yêu cầu Chờ duyệt (000 T094; BR-M15-09, FR-031a): mốc nhắc CFG-M15-05 (hoặc tham số riêng của loại), mốc báo
 * CFG-M15-06, tính từ thời điểm gửi duyệt. Danh sách người duyệt đã lọc theo giới hạn vai trò; người duyệt đều là Quản lý
 * viện thì không báo ở mốc CFG-M15-06; không còn người duyệt thì báo Quản lý viện ngay ở mốc nhắc.
 */
@Component
class ReminderTransaction {

    private final ApprovalRequestStore store;
    private final ApproverDirectory directory;
    private final OverdueReminder reminder;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    ReminderTransaction(
            ApprovalRequestStore store,
            ApproverDirectory directory,
            OverdueReminder reminder,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.store = store;
        this.directory = directory;
        this.reminder = reminder;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
    }

    /** Trả {@code true} khi lần này đã nhắc hoặc đã báo Quản lý viện. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean remind(long id, long schedulerRunId) {
        ApprovalRequestView request = store.lock(id).orElse(null);
        if (request == null || !ApprovalStatus.PENDING_APPROVAL.equals(request.status())) {
            return false;
        }
        RequestTypeHandler handler = directory.handler(request.type());
        List<Long> approvers = directory.approvers(request, handler);
        ParameterCode remindCode = handler.reminderParameter().orElse(ParameterCode.CFG_M15_05);
        Decision decision = reminder.check(
                request.submittedAt(),
                request.remindedAt(),
                request.escalatedAt(),
                remindCode,
                ParameterCode.CFG_M15_06,
                !approvers.isEmpty());
        Instant now = time.now();
        boolean acted = false;
        if (decision.remind()) {
            store.updateWithoutVersion(id, "reminded_at = ?", ApprovalRequestStore.utc(now));
            log(ApprovalActions.REMINDED, request, remindCode, schedulerRunId, Map.of("approvers", approvers));
            events.publishEvent(new ApproverReminderDue(store.find(id).orElseThrow(), approvers, now));
            acted = true;
        }
        EscalationReason escalation = decision.escalation();
        if (escalation == EscalationReason.OVERDUE && !directory.allManagers(approvers)) {
            store.updateWithoutVersion(id, "escalated_at = ?", ApprovalRequestStore.utc(now));
            log(
                    ApprovalActions.ESCALATED,
                    request,
                    ParameterCode.CFG_M15_06,
                    schedulerRunId,
                    Map.of("reason", escalation.name()));
            events.publishEvent(new ManagerEscalationDue(store.find(id).orElseThrow(), escalation, now));
            acted = true;
        } else if (escalation == EscalationReason.NO_APPROVER_LEFT) {
            store.updateWithoutVersion(
                    id,
                    "reminded_at = ?, escalated_at = ?",
                    ApprovalRequestStore.utc(now),
                    ApprovalRequestStore.utc(now));
            log(ApprovalActions.ESCALATED, request, remindCode, schedulerRunId, Map.of("reason", escalation.name()));
            events.publishEvent(new ManagerEscalationDue(store.find(id).orElseThrow(), escalation, now));
            acted = true;
        }
        return acted;
    }

    private void log(
            AuditAction action,
            ApprovalRequestView request,
            ParameterCode rule,
            long schedulerRunId,
            Map<String, Object> details) {
        auditLog.write(AuditLogEntry.builder()
                .action(action)
                .entity(ApprovalActions.ENTITY, request.id())
                .resident(request.target().residentId())
                .basis(new Basis(rule.code(), request.id(), null, schedulerRunId))
                .details(details)
                .build());
    }
}
