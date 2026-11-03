package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.api.events.VitalSignConfirmed;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe các sự kiện chỉ số, thuốc, chăm sóc để tạo hoặc đóng nguồn cảnh báo (T017, FR-027, S-09).
 */
@Component
public class AlertRequestListener {

    private final AlertService alertService;

    public AlertRequestListener(AlertService alertService) {
        this.alertService = alertService;
    }

    @ApplicationModuleListener
    public void onVitalSignConfirmed(VitalSignConfirmed event) {
        if (event.level() == null
                || event.level() == VitalSignLevel.NORMAL
                || event.level() == VitalSignLevel.NO_THRESHOLD) {
            return;
        }

        IncidentLevel incidentLevel = switch (event.level()) {
            case WARNING -> IncidentLevel.MEDIUM;
            case DANGEROUS -> IncidentLevel.CRITICAL;
            default -> null;
        };

        if (incidentLevel == null) {
            return;
        }

        var command = new CreateAlertCommand(
                event.residentId(),
                null,
                "CHI_SO",
                TypeLock.forVitalSign(event.indicatorId()),
                incidentLevel,
                event.measuredAt() != null ? event.measuredAt() : event.occurredAt(),
                "CHI_SO",
                event.recordId(),
                "Chỉ số vượt ngưỡng " + event.level().name(),
                null);

        alertService.requestAlert(command);
    }
}
