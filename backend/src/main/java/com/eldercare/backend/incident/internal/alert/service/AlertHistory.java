package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Lịch sử cảnh báo (nhóm 3, bảng chung {@code record_event} kind {@code M05.ALERT_HISTORY}; research S-08, R-15): mỗi
 * dòng một lần leo thang, tiếp nhận, xử lý, đổi mức, giao, đóng, chuyển người phụ trách. Không có bean ghi nhận chung
 * thì bỏ qua (test đơn vị).
 */
final class AlertHistory {
    static final SharedKind<Map> KIND = SharedKind.of("M05.ALERT_HISTORY", Map.class);

    private final RecordEventApi records;

    AlertHistory(RecordEventApi records) {
        this.records = records;
    }

    /** Khóa duy nhất theo cảnh báo, hành động và thời điểm, nên lần chạy lại cùng mốc không ghi trùng. */
    void add(long alertId, String action, Map<String, Object> detail, Instant at) {
        if (records == null) return;
        records.add(
                KIND,
                new EntityRef(AlertNotices.SOURCE_TYPE, alertId),
                new NewRecordEvent<>(
                        Map.<String, Object>of("action", action, "detail", detail),
                        null,
                        null,
                        true,
                        at,
                        at,
                        at,
                        null,
                        "ALERT_HISTORY:" + alertId + ":" + action + ":" + at.toEpochMilli()));
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> list(long alertId) {
        if (records == null) return List.of();
        return records.list(KIND, new EntityRef(AlertNotices.SOURCE_TYPE, alertId)).stream()
                .map(event -> (Map<String, Object>) event.content())
                .toList();
    }
}
