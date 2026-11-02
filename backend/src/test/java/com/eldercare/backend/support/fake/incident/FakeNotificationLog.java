package com.eldercare.backend.support.fake.incident;

import com.eldercare.backend.incident.api.dto.IncidentNotificationView;
import com.eldercare.backend.incident.api.spi.NotificationLogPort;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Nhật ký thông báo do test đặt, không gửi thông báo thật. */
public class FakeNotificationLog implements NotificationLogPort {
    private record Source(String type, long id) {}

    private final Map<Source, List<IncidentNotificationView>> rows = new HashMap<>();

    public FakeNotificationLog source(String type, long id, List<IncidentNotificationView> notifications) {
        rows.put(new Source(type, id), List.copyOf(notifications));
        return this;
    }

    @Override
    public List<IncidentNotificationView> bySource(String sourceRecordType, long sourceRecordId) {
        return rows.getOrDefault(new Source(sourceRecordType, sourceRecordId), List.of());
    }
}
