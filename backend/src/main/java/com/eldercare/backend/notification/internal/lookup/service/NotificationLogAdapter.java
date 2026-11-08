package com.eldercare.backend.notification.internal.lookup.service;

import com.eldercare.backend.incident.api.dto.IncidentNotificationView;
import com.eldercare.backend.incident.api.spi.NotificationLogPort;
import com.eldercare.backend.notification.api.NotificationLookupApi;
import com.eldercare.backend.notification.api.dto.NotifiedPersonView;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * {@code NhatKyThongBaoPort} của 007 do 009 cài (FR-040, FR-048b, Q-388; module-interfaces 007 mục 2.4): đọc người đã được
 * thông báo theo bản ghi nguồn, một dòng cho mỗi người nhận trên mỗi kênh đã gửi, giữ mã loại người nhận và kênh của 009,
 * mốc gửi, xem, và đủ chuỗi nhắc gọi (kể cả lượt gọi người thân kế tiếp, giữ thông báo của người đầu chưa xác nhận).
 * Chưa có lần gửi qua kênh nào thì là một dòng {@code IN_APP}: thông báo nào cũng có trong hộp thư.
 */
@Component
class NotificationLogAdapter implements NotificationLogPort {

    static final String IN_APP = "IN_APP";

    private final NotificationLookupApi lookup;

    NotificationLogAdapter(NotificationLookupApi lookup) {
        this.lookup = lookup;
    }

    @Override
    public List<IncidentNotificationView> bySource(String sourceRecordType, long sourceRecordId) {
        List<IncidentNotificationView> rows = new ArrayList<>();
        for (NotifiedPersonView person : lookup.bySource(sourceRecordType, Long.toString(sourceRecordId))) {
            Set<String> channels = new LinkedHashSet<>();
            person.channels().forEach(c -> channels.add(c.channel()));
            if (channels.isEmpty()) {
                channels.add(IN_APP);
            }
            List<IncidentNotificationView.CallReminderSummary> reminders = person.callReminders().stream()
                    .map(r -> new IncidentNotificationView.CallReminderSummary(
                            r.callReminderId(),
                            r.personToCallType().name(),
                            r.personToCallId(),
                            r.status(),
                            r.results().stream()
                                    .map(o -> new IncidentNotificationView.CallOutcome(o.result(), o.calledAt()))
                                    .toList()))
                    .toList();
            for (String channel : channels) {
                rows.add(new IncidentNotificationView(
                        person.recipientType().name(),
                        person.recipientId(),
                        channel,
                        person.sentAt(),
                        person.viewedAt(),
                        reminders));
            }
        }
        return rows;
    }
}
