package com.cambae.backend.m13.web.response;

import com.cambae.backend.common.notification.InformationSubject;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.Q354Case;
import com.cambae.backend.m13.internal.lookup.service.NotificationLookupService.HistoryEntry;
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
        List<Map<String, Object>> callReminders,
        List<String> flags) {

    public record Channel(String channel, String result, Instant occurredAt) {}

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
                List.of(),
                r.flags());
    }
}
