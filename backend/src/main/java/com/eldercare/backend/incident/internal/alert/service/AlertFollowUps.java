package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Kết quả theo dõi của cảnh báo đang mở (FR-087, Q-443): nhóm 3, bảng chung {@code record_event} kind
 * {@code M05.ALERT_FOLLOW_UP}, chỉ ghi thêm. Không có bean ghi nhận chung thì bỏ qua (test đơn vị).
 */
final class AlertFollowUps {
    static final SharedKind<Map> KIND = SharedKind.of("M05.ALERT_FOLLOW_UP", Map.class);

    /** Một dòng theo dõi đã đọc: người ghi là mã tài khoản. */
    record Row(String content, Instant at, long recordedBy) {}

    private final RecordEventApi records;

    AlertFollowUps(RecordEventApi records) {
        this.records = records;
    }

    void add(long alertId, String content, long accountId, Instant at) {
        if (records == null) return;
        records.add(
                KIND,
                new EntityRef(AlertNotices.SOURCE_TYPE, alertId),
                new NewRecordEvent<>(
                        Map.<String, Object>of("content", content, "by", accountId),
                        null,
                        null,
                        true,
                        at,
                        at,
                        at,
                        null,
                        "ALERT_FOLLOW_UP:" + alertId + ":" + accountId + ":" + at.toEpochMilli()));
    }

    List<Row> list(long alertId) {
        if (records == null) return List.of();
        return records.list(KIND, new EntityRef(AlertNotices.SOURCE_TYPE, alertId)).stream()
                .map(event -> {
                    Map<?, ?> content = event.content();
                    return new Row(
                            (String) content.get("content"),
                            event.occurredAt(),
                            ((Number) content.get("by")).longValue());
                })
                .toList();
    }
}
