package com.eldercare.backend.incident.internal.incident.domain;

import com.eldercare.backend.incident.api.dto.IncidentNotificationView;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Sự cố kèm phần đọc cho màn hình (api-007 {@code Incident}): diễn biến, người xử lý, người được thông báo, lệnh. */
public record IncidentDetails(
        IncidentView incident,
        String assigneeName,
        List<Map<String, Object>> progressions,
        List<Map<String, Object>> levelChanges,
        List<String> handlers,
        List<IncidentNotificationView> notified,
        Set<String> allowedCommands) {
    public IncidentDetails {
        progressions = List.copyOf(progressions);
        levelChanges = List.copyOf(levelChanges);
        handlers = List.copyOf(handlers);
        notified = List.copyOf(notified);
        allowedCommands = Set.copyOf(allowedCommands);
    }
}
