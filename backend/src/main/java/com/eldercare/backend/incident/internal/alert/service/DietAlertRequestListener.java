package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.incident.api.events.AlertRequested;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Nơi nhận {@link AlertRequested} của 011 loại {@code DIET_ALLERGY_CONFLICT} (nguồn {@code MENU_DISH}): thực đơn có món
 * gây dị ứng mà không có món thay phù hợp (011 FR-021, FR-065). Tạo cảnh báo mức Trung bình theo món, báo Bác sĩ trực;
 * mỗi {@code eventKey} chỉ tính một lần gộp.
 */
@Component
public class DietAlertRequestListener {
    static final String TYPE = "DIET_ALLERGY_CONFLICT";
    private static final String MARKER_KIND = "M05.DIET_ALLERGY_ALERT";

    private final AlertService alerts;
    private final ReminderMarkers markers;

    public DietAlertRequestListener(AlertService alerts, ReminderMarkers markers) {
        this.alerts = alerts;
        this.markers = markers;
    }

    @ApplicationModuleListener
    public void onAlertRequested(AlertRequested event) {
        if (!TYPE.equals(event.type())) return;
        if (!markers.mark(MARKER_KIND, "M01.RESIDENT", event.residentId(), event.eventKey(), null)) return;
        alerts.requestAlert(
                new CreateAlertCommand(
                        event.residentId(),
                        null,
                        TYPE,
                        TypeLock.forDietAllergy(event.sourceId()),
                        IncidentLevel.MEDIUM,
                        event.occurredAt(),
                        event.sourceType(),
                        event.sourceId(),
                        "Thực đơn có món gây dị ứng, chưa có món thay phù hợp",
                        null),
                alert -> List.of(doctorNotice(alert, event)));
    }

    private static NotificationRequest doctorNotice(
            com.eldercare.backend.incident.internal.alert.domain.AlertRecord alert, AlertRequested event) {
        return new NotificationRequest(
                "M05",
                "DIET_ALLERGY_CONFLICT_ALERT",
                SourceGroup.HEALTH_ALERT,
                new SourceRecord(AlertNotices.SOURCE_TYPE, Long.toString(alert.id())),
                AlertNotices.SOURCE_TYPE + ":" + alert.id() + ":DIET_ALLERGY",
                Optional.of(NotificationLevel.MEDIUM),
                Optional.ofNullable(alert.residentId()),
                Optional.empty(),
                List.of(new RecipientGroup.OnDutyDoctor(new Substitution.Default())),
                List.of(new ContentPart(
                        InformationType.HEALTH,
                        "Thực đơn có món gây dị ứng, chưa có món thay phù hợp. Mở cảnh báo để xem.")),
                Optional.of("Thực đơn xung đột dị ứng"),
                Optional.of("Cảnh báo Trung bình"),
                Optional.empty(),
                false,
                event.occurredAt(),
                Map.of("alertId", alert.id()),
                false,
                Optional.empty(),
                Optional.empty());
    }
}
