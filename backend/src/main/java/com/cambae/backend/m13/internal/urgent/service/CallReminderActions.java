package com.cambae.backend.m13.internal.urgent.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m13.api.dto.AcknowledgementMethod;
import com.cambae.backend.m13.api.dto.RecipientType;
import com.cambae.backend.m13.api.events.NotificationUnreachable;
import com.cambae.backend.m13.api.events.UrgentNotificationAcknowledged;
import com.cambae.backend.m13.internal.domain.NotificationEventType;
import com.cambae.backend.m13.internal.persistence.NotificationStore;
import com.cambae.backend.m13.internal.urgent.persistence.CallReminderStore;
import com.cambae.backend.m13.internal.urgent.persistence.CallReminderStore.CallResultRow;
import com.cambae.backend.m13.internal.urgent.persistence.CallReminderStore.ContentItem;
import com.cambae.backend.m13.internal.urgent.persistence.CallReminderStore.Reminder;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thao tác dùng chung của lượt nhắc gọi, chạy trong transaction của người gọi (FR-027 → FR-030a, FR-044): giao lượt cho
 * người được nhắc, dừng khi người nhận tự xác nhận, xác nhận qua cuộc gọi, hết người trong chuỗi gọi. "Thông báo của chuỗi"
 * là mọi thông báo gộp vào các lượt của chuỗi; lượt sinh từ FR-029a (người thân kế tiếp) không gộp thêm thông báo.
 */
@Component
class CallReminderActions {

    /** Số cuộc gọi tối đa kèm trong thông báo báo Quản lý viện (thông số kỹ thuật). */
    static final int CALLS_LIMIT = 20;

    private static final DateTimeFormatter HOUR =
            DateTimeFormatter.ofPattern("HH:mm").withZone(BusinessTime.ZONE);
    private static final Map<String, String> REASONS = Map.of(
            "NO_ANSWER", "không nghe máy",
            "WRONG_NUMBER", "sai số",
            "NO_NUMBER", "không có số",
            "OTHER", "lý do khác");

    private final CallReminderStore store;
    private final NotificationStore notifications;
    private final CallReminderNotices notices;
    private final CallReminderRouting routing;
    private final ApplicationEventPublisher events;

    CallReminderActions(
            CallReminderStore store,
            NotificationStore notifications,
            CallReminderNotices notices,
            CallReminderRouting routing,
            ApplicationEventPublisher events) {
        this.store = store;
        this.notifications = notifications;
        this.notices = notices;
        this.routing = routing;
        this.events = events;
    }

    /** Giao lượt: ghi chuyển nhắc, nhắc người mới (Khẩn cấp); tới bậc Quản lý viện thì báo các Quản lý viện khác. */
    void assign(
            long reminderId,
            Long previousStaffId,
            CallReminderRouting.Assignment to,
            String reason,
            Instant due,
            Instant at) {
        long handoff = store.handoff(reminderId, previousStaffId, to.staffId(), to.tier(), reason, due, at);
        Reminder r = store.find(reminderId).orElseThrow();
        notices.remind(r, to.staffId(), handoff, contentToReport(r.chainId()), at);
        if (!to.otherManagers().isEmpty()) {
            notices.informOtherManagers(r, to.staffId(), Long.toString(handoff), at);
        }
    }

    /** Nội dung cần báo của chuỗi, theo thứ tự thời gian (FR-027, FR-027a). */
    String contentToReport(long chainId) {
        return store.chainContent(chainId).stream()
                .map(ContentItem::content)
                .map(c -> c == null || c.isBlank() ? "(không có nội dung rút gọn)" : c)
                .collect(Collectors.joining("; "));
    }

    /**
     * Người nhận tự xác nhận (FR-030): lượt Chờ kết quả của chuỗi có thông báo này dừng khi mọi thông báo của chuỗi đã xác
     * nhận; người được nhắc được báo không cần gọi.
     */
    void afterAcknowledged(long notificationId, Instant at) {
        for (long reminderId : store.remindersOf(notificationId)) {
            Reminder linked = store.find(reminderId).orElseThrow();
            if (store.chainUnacknowledged(linked.chainId()) > 0) {
                continue;
            }
            for (long awaiting : store.awaitingInChain(linked.chainId())) {
                Reminder r = store.lock(awaiting).orElseThrow();
                if (!"AWAITING_RESULT".equals(r.status())) {
                    continue;
                }
                store.finish(r.id(), "STOPPED", null, "SELF_ACKNOWLEDGED");
                notices.inform(
                        r,
                        List.of(r.currentStaffId()),
                        "CALL_REMINDER_STOPPED",
                        "SELF_ACKNOWLEDGED",
                        "Không cần gọi: người nhận đã tự xác nhận thông báo khẩn cấp",
                        at);
            }
        }
    }

    /**
     * Đã liên lạc được (FR-030, FR-044): thông báo gộp vào chính lượt vừa gọi ghi xác nhận qua cuộc gọi; thông báo đang Không
     * liên lạc được thì xác nhận muộn qua cuộc gọi; báo module nguồn. Gọi được người thân kế tiếp (lượt không gộp thông báo)
     * thì thông báo của người cần gọi trước giữ nguyên, vì chính họ chưa biết tin (FR-026; chủ dự án chọn 2026-10-27).
     */
    void acknowledgeByCall(long reminderId, long actorAccountId, Instant at) {
        for (long id : store.notificationsOf(reminderId)) {
            Optional<String> status = notifications.lockStatus(id);
            if (status.filter("ACKNOWLEDGED"::equals).isPresent()) {
                continue;
            }
            AcknowledgementMethod method = status.filter("UNREACHABLE"::equals).isPresent()
                    ? AcknowledgementMethod.LATE_BY_CALL
                    : AcknowledgementMethod.BY_CALL;
            notifications.appendEvent(
                    id,
                    NotificationEventType.ACKNOWLEDGE,
                    Optional.of(method),
                    Optional.empty(),
                    null,
                    actorAccountId,
                    at);
            events.publishEvent(new UrgentNotificationAcknowledged(id, store.source(id), method));
        }
    }

    /**
     * Hết người để gọi (FR-029a): không còn lượt Chờ kết quả nào của chuỗi thì thông báo của chuỗi chuyển Không liên lạc
     * được và báo module nguồn; Quản lý viện nhận Khẩn cấp kèm các cuộc gọi, trừ khi chính họ là người cần gọi.
     */
    void chainNotReached(Reminder r, Instant at) {
        if (store.chainAwaiting(r.chainId())) {
            return;
        }
        for (long id : store.chainNotifications(r.chainId())) {
            Optional<String> status = notifications.lockStatus(id);
            if (status.filter(s -> s.equals("ACKNOWLEDGED") || s.equals("UNREACHABLE"))
                    .isPresent()) {
                continue;
            }
            notifications.appendEvent(
                    id,
                    NotificationEventType.UNREACHABLE,
                    Optional.empty(),
                    Optional.empty(),
                    "CALL_NOT_REACHED",
                    null,
                    at);
            events.publishEvent(new NotificationUnreachable(id, store.source(id)));
        }
        Optional<Long> calleeStaff =
                r.calleeType() == RecipientType.STAFF ? Optional.of(r.calleeId()) : Optional.empty();
        // Giới hạn số cuộc gọi kèm theo để thông báo vừa outbox; đủ danh sách ở chi tiết từng lượt
        String calls = store.chainResults(r.chainId()).stream()
                .limit(CALLS_LIMIT)
                .map(CallReminderActions::describe)
                .collect(Collectors.joining("; "));
        // Người cần gọi là Quản lý viện thì chỉ báo các Quản lý viện khác (FR-029a (b))
        boolean calleeIsManager = calleeStaff
                .filter(routing.managerStaffIds(Optional.empty())::contains)
                .isPresent();
        notices.chainNotReached(
                r, calleeIsManager ? Optional.of(routing.managerStaffIds(calleeStaff)) : Optional.empty(), calls, at);
    }

    private static String describe(CallResultRow c) {
        String outcome = "REACHED".equals(c.result())
                ? "đã liên lạc được"
                : "không liên lạc được (" + REASONS.getOrDefault(c.reason(), c.reason()) + ")";
        return HOUR.format(c.calledAt()) + " gọi " + (c.calledPhoneNumber() == null ? "" : c.calledPhoneNumber() + " ")
                + outcome;
    }
}
