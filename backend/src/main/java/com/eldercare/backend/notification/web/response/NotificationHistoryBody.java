package com.eldercare.backend.notification.web.response;

import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.notification.internal.lookup.service.NotificationLookupService.HistoryEntry;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Một dòng lịch sử thông báo theo schema {@code NotificationHistoryEntry} (FR-037): một người nhận của một yêu cầu. Lượt
 * nhắc gọi làm ở US1 nên hiện rỗng.
 */
public record NotificationHistoryBody(
        long requestId,
        String triggerSource,
        Map<String, String> sourceRecord,
        NotificationLevel level,
        String recipient,
        List<String> basis,
        InformationSubject informationSubject,
        Q354Case q354Case,
        Instant receivedAt,
        Instant sentAt,
        List<Channel> channels,
        Instant viewedAt,
        Instant acknowledgedAt,
        List<CallReminderEntry> callReminders,
        List<String> flags) {

    public record Channel(String channel, String result, Instant occurredAt) {}

    /** Lượt nhắc gọi của chuỗi chứa thông báo và kết quả (FR-037). */
    public record CallReminderEntry(
            long id, String personToCallType, long personToCallId, String status, List<CallOutcome> results) {}

    public record CallOutcome(String result, Instant calledAt) {}

    public static NotificationHistoryBody of(HistoryEntry e) {
        var r = e.row();
        return new NotificationHistoryBody(
                r.requestId(),
                r.sourceGroup(),
                Map.of("type", r.sourceRecordType(), "id", r.sourceRecordId()),
                r.level(),
                r.recipientType() + ":" + r.recipientId(),
                r.basis(),
                r.informationSubject(),
                r.q354Case(),
                r.receivedAt(),
                r.sentAt(),
                e.channels().stream()
                        .map(c -> new Channel(c.channel(), c.result(), c.at()))
                        .toList(),
                r.viewedAt(),
                r.acknowledgedAt(),
                e.callReminders().stream()
                        .map(c -> new CallReminderEntry(
                                c.callReminderId(),
                                c.personToCallType().name(),
                                c.personToCallId(),
                                c.status(),
                                c.results().stream()
                                        .map(o -> new CallOutcome(o.result(), o.calledAt()))
                                        .toList()))
                        .toList(),
                r.flags());
    }
}
