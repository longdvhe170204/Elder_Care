package com.eldercare.backend.notification.internal.urgent.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditAction;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.api.dto.AcknowledgementMethod;
import com.eldercare.backend.notification.api.events.UrgentNotificationAcknowledged;
import com.eldercare.backend.notification.internal.domain.NotificationEventType;
import com.eldercare.backend.notification.internal.inbox.domain.InboxItem;
import com.eldercare.backend.notification.internal.inbox.persistence.InboxStore;
import com.eldercare.backend.notification.internal.inbox.service.InboxService;
import com.eldercare.backend.notification.internal.persistence.NotificationStore;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Xác nhận đã nhận" thông báo Khẩn cấp (tên logic {@code KhanCapService}, phần xác nhận; FR-026, FR-026a, research B-07):
 * chỉ người nhận, chỉ mức Khẩn cấp, một lần; đang "Không liên lạc được" thì ghi xác nhận muộn (Q-104). Không đổi bản ghi
 * nguồn; phát {@link UrgentNotificationAcknowledged} cho module nguồn (FR-035). Mọi thông báo của chuỗi gọi đã xác nhận thì
 * lượt nhắc gọi Chờ kết quả dừng (FR-030).
 */
@Service
public class UrgentAcknowledgement {

    static final AuditAction ACKNOWLEDGED = new AuditAction("NOTIFICATION_ACKNOWLEDGED", false);
    static final String ENTITY = "M13.NOTIFICATION";

    private final InboxStore inbox;
    private final NotificationStore notifications;
    private final ObjectProvider<CurrentUser> currentUser;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final CallReminderActions reminders;
    private final BusinessTime time;

    UrgentAcknowledgement(
            InboxStore inbox,
            NotificationStore notifications,
            ObjectProvider<CurrentUser> currentUser,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            CallReminderActions reminders,
            BusinessTime time) {
        this.inbox = inbox;
        this.notifications = notifications;
        this.currentUser = currentUser;
        this.auditLog = auditLog;
        this.events = events;
        this.reminders = reminders;
        this.time = time;
    }

    @BusinessCommand(requiresReason = false)
    @Transactional
    public void acknowledge(long notificationId) {
        CurrentUser user = currentUser.getObject();
        InboxItem row =
                inbox.find(notificationId, InboxService.recipientKey(user)).orElseThrow(NotFoundException::new);
        if (row.level() != NotificationLevel.URGENT) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "NOT_URGENT", "Chỉ thông báo Khẩn cấp cần xác nhận đã nhận");
        }
        Optional<String> status = notifications.lockStatus(notificationId);
        if (status.filter("ACKNOWLEDGED"::equals).isPresent()) {
            throw alreadyAcknowledged();
        }
        AcknowledgementMethod method = status.filter("UNREACHABLE"::equals).isPresent()
                ? AcknowledgementMethod.LATE
                : AcknowledgementMethod.IN_APP;
        Instant now = time.now();
        try {
            notifications.appendEvent(
                    notificationId,
                    NotificationEventType.ACKNOWLEDGE,
                    Optional.of(method),
                    Optional.empty(),
                    null,
                    user.accountId(),
                    now);
        } catch (DuplicateKeyException e) {
            throw alreadyAcknowledged();
        }
        reminders.afterAcknowledged(notificationId, now);
        auditLog.write(AuditLogEntry.builder()
                .action(ACKNOWLEDGED)
                .entity(ENTITY, notificationId)
                .resident(row.residentId())
                .after(Map.of("acknowledgementMethod", method.name()))
                .build());
        events.publishEvent(new UrgentNotificationAcknowledged(
                notificationId, new SourceRecord(row.sourceRecordType(), row.sourceRecordId()), method));
    }

    private static BusinessException alreadyAcknowledged() {
        return new BusinessException(HttpStatus.CONFLICT, "ALREADY_ACKNOWLEDGED", "Thông báo đã được xác nhận");
    }
}
