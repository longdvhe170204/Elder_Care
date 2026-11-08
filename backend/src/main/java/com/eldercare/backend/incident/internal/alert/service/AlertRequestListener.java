package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.care.api.events.AlertRequested;
import com.eldercare.backend.common.data.ReminderMarkers;
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
    private final ReminderMarkers markers;

    public AlertRequestListener(AlertService alertService, ReminderMarkers markers) {
        this.alertService = alertService;
        this.markers = markers;
    }

    /** Cảnh báo chăm sóc đã có nguồn và milestone ổn định ở 005; xử lý lại không tăng số lần gộp. */
    @ApplicationModuleListener
    public void onCareAlertRequested(AlertRequested event) {
        if (!switch (event.type()) {
            case "LOW_WATER_INTAKE", "PROLONGED_POOR_EATING", "PROLONGED_NEGATIVE_MOOD" -> true;
            default -> false;
        }) return;
        if (!markers.mark("M05.CARE_ALERT", "M01.RESIDENT", event.residentId(), event.eventKey(), null)) return;
        var level = switch (event.level()) {
            case LOW -> IncidentLevel.LOW;
            case MEDIUM -> IncidentLevel.MEDIUM;
            case URGENT -> IncidentLevel.CRITICAL;
        };
        var command = new CreateAlertCommand(
                event.residentId(),
                event.context().floorAreaId().orElse(null),
                event.type(),
                event.type(),
                level,
                event.context().occurredAt(),
                event.context().sourceType(),
                event.context().sourceId(),
                switch (event.type()) {
                    case "LOW_WATER_INTAKE" -> "Uống nước chưa đủ";
                    case "PROLONGED_POOR_EATING" -> "Ăn kém kéo dài";
                    default -> "Tâm trạng tiêu cực kéo dài";
                },
                null);
        alertService.requestAlert(command);
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
                null,
                null,
                event.recordedById());

        alertService.requestAlert(command);
    }
}
