package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.care.api.events.AlertClosureRequested;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.AlertSourceCloseRequested;
import com.eldercare.backend.incident.api.events.IncidentNotificationCreated;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentProgressionService;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yêu cầu đóng cảnh báo theo nguồn (FR-032, research S-09): đánh dấu nguồn đã xử lý; mọi nguồn của cảnh báo đã xử lý thì
 * cảnh báo đang mở Đã đóng bởi Hệ thống, cảnh báo đã Chuyển sự cố giữ nguyên và sự cố liên kết nhận diễn biến "Nguồn đã xử
 * lý" (báo người xử lý, Nhẹ). Đính chính bản ghi nguồn không phải là xử lý nguồn (FR-019).
 */
@Component
public class AlertSourceResolver {
    private static final SharedKind<Map> RESOLVED = SharedKind.of("M05.ALERT_SOURCE_RESOLVED", Map.class);
    private static final String ALERT = "M05.ALERT";

    private final AlertStore alerts;
    private final AlertService alertService;
    private final RecordEventApi recordEvents;
    private final IncidentProgressionService progressions;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public AlertSourceResolver(
            AlertStore alerts,
            AlertService alertService,
            RecordEventApi recordEvents,
            IncidentProgressionService progressions,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.alerts = alerts;
        this.alertService = alertService;
        this.recordEvents = recordEvents;
        this.progressions = progressions;
        this.events = events;
        this.time = time;
    }

    /** Yêu cầu của 011, 014 và module làm plan sau (module-interfaces 1.13). */
    @ApplicationModuleListener
    void onSourceCloseRequested(AlertSourceCloseRequested event) {
        resolve(event.sourceType(), event.sourceId(), event.reason(), time.now());
    }

    /** Công việc của 005 có kết quả (đúng giờ hoặc muộn): nguồn công việc của cảnh báo đã được xử lý. */
    @ApplicationModuleListener
    void onCareClosureRequested(AlertClosureRequested event) {
        var context = event.context();
        resolve(context.sourceType(), context.sourceId(), event.reason(), context.occurredAt());
    }

    @Transactional
    public void resolve(String sourceType, long sourceId, String reason, Instant at) {
        for (long[] row : alerts.bySource(sourceType, List.of(sourceId))) {
            long alertId = row[1];
            var alert = alerts.findById(alertId).orElse(null);
            if (alert == null) continue;
            boolean open = alert.status() != AlertStatus.CLOSED && alert.status() != AlertStatus.CONVERTED_TO_INCIDENT;
            if (!open && alert.status() != AlertStatus.CONVERTED_TO_INCIDENT) continue;
            if (!markResolved(alertId, sourceType, sourceId, at)) continue;
            if (!allSourcesResolved(alertId)) continue;
            if (open) {
                if (alert.residentId() != null) {
                    alertService.closeBySystem(
                            alert.residentId(), alert.typeLock(), "Nguồn đã được xử lý: " + reason, at);
                }
            } else {
                notifyIncident(alert, reason, at);
            }
        }
    }

    private void notifyIncident(AlertRecord alert, String reason, Instant at) {
        var notices =
                progressions.addBySystem(alert.incidentId(), "SOURCE_RESOLVED", "Nguồn đã được xử lý: " + reason, at);
        if (notices.isEmpty()) return;
        events.publishEvent(new IncidentNotificationCreated(
                alert.incidentId(),
                notices.get(0).level().map(Enum::name).orElse("LOW"),
                notices.get(0).eventKey(),
                at,
                notices));
    }

    /** Đánh dấu nguồn đã xử lý; {@code false} khi đã đánh dấu (giao lại sự kiện). */
    private boolean markResolved(long alertId, String sourceType, long sourceId, Instant at) {
        String key = "ALERT_SOURCE_RESOLVED:" + alertId + ":" + sourceType + ":" + sourceId;
        if (recordEvents.findByUniqueKey(RESOLVED, key).isPresent()) return false;
        recordEvents.add(
                RESOLVED,
                new EntityRef(ALERT, alertId),
                new NewRecordEvent<>(
                        Map.of("sourceType", sourceType, "sourceId", sourceId),
                        null,
                        null,
                        true,
                        at,
                        at,
                        at,
                        null,
                        key));
        return true;
    }

    private boolean allSourcesResolved(long alertId) {
        var parent = new EntityRef(ALERT, alertId);
        Set<String> resolved = new HashSet<>();
        for (var event : recordEvents.list(RESOLVED, parent)) resolved.add(sourceKey(event.content()));
        for (var event : recordEvents.list(AlertQueryService.SOURCE, parent)) {
            if (!resolved.contains(sourceKey(event.content()))) return false;
        }
        return true;
    }

    private static String sourceKey(Map<?, ?> content) {
        return content.get("sourceType") + ":" + ((Number) content.get("sourceId")).longValue();
    }
}
