package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.health.api.events.VitalSignCorrected;
import com.eldercare.backend.incident.api.events.AlertSourceCorrected;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Chỉ số làm nguồn cảnh báo bị đính chính (FR-019, BR-M06-06): cảnh báo đã tạo không tự đóng, chỉ báo người phụ trách
 * để xem xét lại.
 */
@Component
public class AlertSourceCorrectionListener {
    private static final Set<AlertStatus> OPEN =
            Set.of(AlertStatus.NEW, AlertStatus.ESCALATED, AlertStatus.RECEIVED, AlertStatus.IN_PROGRESS);

    private final AlertStore alerts;
    private final ApplicationEventPublisher events;

    public AlertSourceCorrectionListener(AlertStore alerts, ApplicationEventPublisher events) {
        this.alerts = alerts;
        this.events = events;
    }

    @ApplicationModuleListener
    public void onVitalSignCorrected(VitalSignCorrected event) {
        for (long[] link : alerts.bySource("CHI_SO", List.of(event.originalRecordId()))) {
            alerts.findById(link[1])
                    .filter(alert -> OPEN.contains(alert.status()))
                    .ifPresent(alert -> events.publishEvent(new AlertSourceCorrected(
                            alert.id(),
                            event.eventKey() + ":" + alert.id(),
                            event.occurredAt(),
                            AlertNotices.sourceCorrected(alert, event.eventKey(), event.occurredAt()))));
        }
    }
}
