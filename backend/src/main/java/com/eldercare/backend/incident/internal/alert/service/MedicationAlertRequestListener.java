package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import com.eldercare.backend.medication.api.events.MedicationAlertRequestedEvent;
import com.eldercare.backend.medication.api.events.ReconciliationCompleted;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Yêu cầu cảnh báo của 006 thành cảnh báo (T017, FR-026 (b), FR-032, research S-09): liều Bỏ lỡ, xung đột dị ứng, phản
 * ứng thuốc, nghi dùng trùng liều, chưa đối chiếu thuốc. "Từ chối liên tiếp" do {@code TrendRuleService} xử lý, nhắc
 * đơn sắp hết hạn là thông báo (FR-017) nên không vào đây. Mỗi {@code deduplicationKey} chỉ tính một lần gộp. Phiếu đối
 * chiếu được xác nhận thì đóng cảnh báo "chưa đối chiếu" của người đó do Hệ thống.
 */
@Component
public class MedicationAlertRequestListener {

    static final String UNRECONCILED = "CHUA_DOI_CHIEU";
    private static final String MARKER_KIND = "M07.MEDICATION_ALERT";
    private static final Map<String, String> DESCRIPTIONS = Map.of(
            "BO_LO_LIEU",
            "Liều thuốc bị bỏ lỡ",
            "XUNG_DOT_DI_UNG",
            "Đơn thuốc xung đột dị ứng",
            "PHAN_UNG_THUOC",
            "Phản ứng sau dùng thuốc",
            "POSSIBLE_DUPLICATE_DOSE",
            "Nghi dùng trùng liều",
            UNRECONCILED,
            "Chưa đối chiếu thuốc");
    private static final Pattern SOURCE = Pattern.compile("([A-Za-z]+):(\\d+)(?:-\\d+)?");

    private final AlertService alerts;
    private final ReminderMarkers markers;

    public MedicationAlertRequestListener(AlertService alerts, ReminderMarkers markers) {
        this.alerts = alerts;
        this.markers = markers;
    }

    @ApplicationModuleListener
    public void onMedicationAlertRequested(MedicationAlertRequestedEvent event) {
        String description = DESCRIPTIONS.get(event.alertType());
        if (description == null) return;
        Matcher source = SOURCE.matcher(event.sourceRecord());
        if (!source.matches()) {
            throw new IllegalArgumentException("Nguồn cảnh báo thuốc không đúng dạng: " + event.sourceRecord());
        }
        if (!markers.mark(MARKER_KIND, "M01.RESIDENT", event.residentId(), event.deduplicationKey(), null)) return;
        long sourceId = Long.parseLong(source.group(2));
        alerts.requestAlert(new CreateAlertCommand(
                event.residentId(),
                null,
                event.alertType(),
                TypeLock.forMedication(event.alertType(), sourceId),
                level(event.alertLevel()),
                event.occurredAt(),
                source.group(1).toUpperCase(),
                sourceId,
                description,
                null));
    }

    /** Phiếu đối chiếu thuốc được xác nhận: nguồn của cảnh báo "chưa đối chiếu" đã được xử lý (research D-13). */
    @ApplicationModuleListener
    public void onReconciliationCompleted(ReconciliationCompleted event) {
        alerts.closeBySystem(
                event.residentId(),
                TypeLock.forMedication(UNRECONCILED, event.residentId()),
                "Nguồn đã được xử lý: phiếu đối chiếu thuốc đã xác nhận",
                event.occurredAt());
    }

    private static IncidentLevel level(String level) {
        return switch (level) {
            case "NHE", "LOW" -> IncidentLevel.LOW;
            case "KHAN_CAP", "CRITICAL" -> IncidentLevel.CRITICAL;
            default -> IncidentLevel.MEDIUM;
        };
    }
}
