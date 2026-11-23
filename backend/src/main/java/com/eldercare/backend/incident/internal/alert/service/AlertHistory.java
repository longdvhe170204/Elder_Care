package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lịch sử cảnh báo (nhóm 3, bảng chung {@code record_event} kind {@code M05.ALERT_HISTORY}; research S-08, R-15): mỗi
 * dòng một lần leo thang, tiếp nhận, xử lý, đổi mức, giao, đóng, chuyển người phụ trách. Không có bean ghi nhận chung
 * thì bỏ qua (test đơn vị).
 */
final class AlertHistory {
    /** Lần báo sau của nhân viên gộp vào cảnh báo thủ công đang mở: giữ người báo và mô tả (FR-030). */
    static final String MERGED = "MERGED";

    /** Người nhận bấm "Tôi đến giúp" ở cảnh báo từ nút SOS, nút hỗ trợ (đề xuất 2026-11-23): một dòng mỗi người. */
    static final String HELPER_JOINED = "HELPER_JOINED";

    /** Người bấm nút hỗ trợ nhắn thêm tình hình (đề xuất 2026-11-23): người nhắn, nội dung, thời điểm. */
    static final String SENDER_MESSAGE = "SENDER_MESSAGE";

    /** Đổi tầng của cảnh báo nút hỗ trợ (đề xuất 2026-11-23): người đổi, tầng cũ, tầng mới (rỗng là khu vực chung). */
    static final String FLOOR_CHANGED = "FLOOR_CHANGED";

    /** Người bấm rút yêu cầu khi chưa ai tiếp nhận (đề xuất 2026-11-23). */
    static final String WITHDRAWN = "WITHDRAWN";

    static final SharedKind<Map> KIND = SharedKind.of("M05.ALERT_HISTORY", Map.class);

    private final RecordEventApi records;

    AlertHistory(RecordEventApi records) {
        this.records = records;
    }

    /** Khóa duy nhất theo cảnh báo, hành động và thời điểm, nên lần chạy lại cùng mốc không ghi trùng. */
    void add(long alertId, String action, Map<String, Object> detail, Instant at) {
        add(alertId, action, detail, at, Long.toString(at.toEpochMilli()));
    }

    /** Như {@link #add(long, String, Map, Instant)} với phần cuối khóa riêng, ví dụ theo người cho "Tôi đến giúp". */
    void add(long alertId, String action, Map<String, Object> detail, Instant at, String keySuffix) {
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
                        "ALERT_HISTORY:" + alertId + ":" + action + ":" + keySuffix));
    }

    /** Tài khoản đã bấm "Tôi đến giúp", theo thứ tự bấm. */
    @SuppressWarnings("unchecked")
    List<Long> helpers(long alertId) {
        return list(alertId).stream()
                .filter(row -> HELPER_JOINED.equals(row.get("action")))
                .map(row -> ((Map<String, Object>) row.get("detail")).get("by"))
                .filter(Objects::nonNull)
                .map(by -> ((Number) by).longValue())
                .distinct()
                .toList();
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> list(long alertId) {
        if (records == null) return List.of();
        return records.list(KIND, new EntityRef(AlertNotices.SOURCE_TYPE, alertId)).stream()
                .map(event -> (Map<String, Object>) event.content())
                .toList();
    }
}
