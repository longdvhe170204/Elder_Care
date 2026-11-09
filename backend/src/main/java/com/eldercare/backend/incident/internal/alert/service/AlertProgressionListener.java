package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.incident.api.events.AlertProgressionRequested;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-086: ghi diễn biến vào cảnh báo đang mở của nguồn theo yêu cầu của module khác (014 FR-047, FR-056), kèm lệnh nguồn,
 * người thực hiện và thời điểm ({@code M05.ALERT_PROGRESSION}, cha {@code M05.ALERT}); không đổi trạng thái cảnh báo. Không
 * có cảnh báo mở của nguồn thì bỏ qua. Khóa sự kiện chống ghi trùng khi outbox giao lại.
 */
@Component
public class AlertProgressionListener {
    static final SharedKind<Map> KIND = SharedKind.of("M05.ALERT_PROGRESSION", Map.class);
    private static final Set<AlertStatus> OPEN =
            Set.of(AlertStatus.NEW, AlertStatus.ESCALATED, AlertStatus.RECEIVED, AlertStatus.IN_PROGRESS);

    private final AlertStore alerts;
    private final RecordEventApi records;

    public AlertProgressionListener(AlertStore alerts, RecordEventApi records) {
        this.alerts = alerts;
        this.records = records;
    }

    @ApplicationModuleListener
    void on(AlertProgressionRequested event) {
        record(event);
    }

    /** Phần xử lý đồng bộ của bộ nghe (test gọi trực tiếp). */
    @Transactional
    public void record(AlertProgressionRequested event) {
        for (long[] row : alerts.bySource(event.sourceType(), List.of(event.sourceId()))) {
            long alertId = row[1];
            var alert = alerts.findById(alertId).orElse(null);
            if (alert == null || !OPEN.contains(alert.status())) continue;
            String key = "ALERT_PROGRESSION:" + alertId + ":" + event.eventKey();
            if (records.findByUniqueKey(KIND, key).isPresent()) continue;
            Map<String, Object> content = new HashMap<>();
            content.put("progressionTypeCode", event.progressionTypeCode());
            content.put("content", event.content());
            content.put("sourceType", event.sourceType());
            content.put("sourceId", event.sourceId());
            records.add(
                    KIND,
                    new EntityRef(AlertNotices.SOURCE_TYPE, alertId),
                    new NewRecordEvent<>(
                            content,
                            null,
                            event.actorId(),
                            event.actorId() == null,
                            event.occurredAt(),
                            null,
                            event.occurredAt(),
                            alert.floorAreaId(),
                            key));
        }
    }
}
