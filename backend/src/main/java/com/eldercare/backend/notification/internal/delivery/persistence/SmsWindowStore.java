package com.eldercare.backend.notification.internal.delivery.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.notification.internal.delivery.domain.BatchedAttempt;
import com.eldercare.backend.notification.internal.delivery.domain.SmsBatchWindowContent;
import com.eldercare.backend.notification.internal.delivery.domain.SmsWindow;
import com.eldercare.backend.notification.internal.persistence.M13SharedRows;
import com.eldercare.backend.notification.internal.persistence.M13Sql;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Khung gộp tin nhắn Trung bình ({@code khung_gop_tin_nhan}; FR-016b) ở {@code simple_record} loại
 * {@code M13.SMS_BATCH_WINDOW} (009 T043, data-model mục 0.1): một khung mở mỗi người nhận nhờ {@code unique_key}.
 */
@Repository
public class SmsWindowStore {

    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final SimpleRecordApi records;
    private final M13SharedRows shared;

    SmsWindowStore(JdbcTemplate jdbc, JsonMapper json, SimpleRecordApi records, M13SharedRows shared) {
        this.jdbc = jdbc;
        this.json = json;
        this.records = records;
        this.shared = shared;
    }

    /** Khung đang mở của người nhận, khóa dòng. */
    public Optional<SmsWindow> openFor(String recipientKey) {
        return jdbc
                .query(
                        "SELECT id, content FROM simple_record WHERE unique_key = ? FOR UPDATE",
                        (rs, i) -> window(rs.getLong(1), json.readValue(rs.getString(2), SmsBatchWindowContent.class)),
                        SmsBatchWindowContent.openKey(recipientKey))
                .stream()
                .findFirst();
    }

    /** Mở khung; người nhận đã có khung mở thì ném {@code DuplicateKeyException}. */
    public long open(String recipientKey, Instant startedAt, Instant endsAt, long firstAttemptId) {
        return shared.createRecord(
                SmsBatchWindowContent.KIND,
                null,
                SmsBatchWindowContent.OPEN,
                new SmsBatchWindowContent(recipientKey, startedAt, endsAt, firstAttemptId, null, null),
                SmsBatchWindowContent.openKey(recipientKey));
    }

    /** Id khung mở đã hết hạn, theo thời điểm hết hạn. */
    public List<Long> due(Instant now) {
        return records.listByStatus(SmsBatchWindowContent.KIND, Set.of(SmsBatchWindowContent.OPEN)).stream()
                .filter(w -> !w.content().endsAt().isAfter(now))
                .sorted(Comparator.comparing(w -> w.content().endsAt()))
                .map(SimpleRecord::id)
                .toList();
    }

    /** Khóa và đọc khung còn mở. */
    public Optional<SmsWindow> lockOpen(long windowId) {
        SimpleRecord<SmsBatchWindowContent> w = records.lockForUpdate(SmsBatchWindowContent.KIND, windowId);
        return SmsBatchWindowContent.OPEN.equals(w.status())
                ? Optional.of(window(w.id(), w.content()))
                : Optional.empty();
    }

    public List<BatchedAttempt> batched(long windowId) {
        return jdbc.query(
                "SELECT a.id, a.entity_id, JSON_VALUE(a.content, '$.address'), q.floor_area_id,"
                        + " COALESCE(q.occurred_at, q.received_at) FROM record_event a"
                        + " JOIN notification n ON n.id = a.entity_id JOIN notification_request q ON q.id = n.request_id"
                        + " WHERE a.m13_batch_window_id = ? AND " + M13Sql.attempts("a")
                        + " AND NOT EXISTS (SELECT 1 FROM record_event r WHERE " + M13Sql.resultOf("r", "a") + ")"
                        + " ORDER BY a.id",
                (rs, i) -> new BatchedAttempt(
                        rs.getLong(1),
                        rs.getLong(2),
                        rs.getString(3),
                        rs.getObject(4, Long.class),
                        rs.getObject(5, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                windowId);
    }

    /** Đóng khung (đã khóa bằng {@link #lockOpen}), nhả khóa khung mở của người nhận. */
    public void close(long windowId, Instant at, Long digestAttemptId) {
        SimpleRecord<SmsBatchWindowContent> w = records.require(SmsBatchWindowContent.KIND, windowId);
        records.change(
                SmsBatchWindowContent.KIND,
                windowId,
                w.version(),
                new RecordChange<>(
                        SmsBatchWindowContent.CLOSED, w.content().closed(at, digestAttemptId), null, null, false));
    }

    private static SmsWindow window(long id, SmsBatchWindowContent c) {
        return new SmsWindow(id, c.recipientKey(), c.endsAt());
    }
}
