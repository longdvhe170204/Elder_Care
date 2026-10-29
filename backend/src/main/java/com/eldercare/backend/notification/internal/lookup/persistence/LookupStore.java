package com.eldercare.backend.notification.internal.lookup.persistence;

import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.notification.api.dto.AcknowledgementMethod;
import com.eldercare.backend.notification.api.dto.NotificationStatus;
import com.eldercare.backend.notification.api.dto.NotifiedPersonView.ChannelResult;
import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.notification.internal.lookup.domain.DeliveryError;
import com.eldercare.backend.notification.internal.lookup.domain.HistoryFilter;
import com.eldercare.backend.notification.internal.lookup.domain.SentNotification;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Đọc lịch sử gửi, dữ liệu cho module nguồn và lỗi gửi (FR-035, FR-037, FR-038; research B-11). */
@Repository
public class LookupStore {

    private static final String SENT = "SELECT n.id, n.request_id, r.source_group, r.source_record_type,"
            + " r.source_record_id, n.level, n.recipient_type, n.recipient_id, n.basis, r.information_subject,"
            + " n.q354_case, r.received_at, s.status, s.sent_at, s.viewed_at, s.acknowledged_at,"
            + " s.acknowledgement_method, r.flags"
            + " FROM notification n JOIN notification_request r ON r.id = n.request_id"
            + " JOIN notification_state s ON s.notification_id = n.id";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public LookupStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<SentNotification> bySource(String sourceRecordType, String sourceRecordId) {
        return jdbc.query(
                SENT + " WHERE r.source_record_type = ? AND r.source_record_id = ? ORDER BY n.id",
                this::sent,
                sourceRecordType,
                sourceRecordId);
    }

    public Optional<Instant> viewedAt(long notificationId) {
        return jdbc
                .query(
                        "SELECT viewed_at FROM notification_state WHERE notification_id = ?",
                        (rs, i) -> instant(rs, "viewed_at"),
                        notificationId)
                .stream()
                .findFirst()
                .map(Optional::ofNullable)
                .flatMap(o -> o);
    }

    public List<SentNotification> history(HistoryFilter f) {
        StringBuilder sql = new StringBuilder(SENT + " WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (f.sourceRecordType() != null) {
            sql.append(" AND r.source_record_type = ?");
            args.add(f.sourceRecordType());
        }
        if (f.sourceRecordId() != null) {
            sql.append(" AND r.source_record_id = ?");
            args.add(f.sourceRecordId());
        }
        if (f.recipientId() != null) {
            sql.append(" AND n.recipient_id = ?");
            args.add(f.recipientId());
        }
        if (f.floorAreaId() != null) {
            sql.append(" AND r.floor_area_id = ?");
            args.add(f.floorAreaId());
        }
        if (f.from() != null) {
            sql.append(" AND r.received_at >= ?");
            args.add(utc(f.from()));
        }
        if (f.to() != null) {
            sql.append(" AND r.received_at < ?");
            args.add(utc(f.to()));
        }
        if (f.floors().isPresent()) {
            Collection<Long> floors = f.floors().get();
            if (floors.isEmpty()) {
                return List.of();
            }
            sql.append(" AND r.floor_area_id IN (")
                    .append(String.join(", ", floors.stream().map(x -> "?").toList()))
                    .append(")");
            args.addAll(floors);
        }
        sql.append(" ORDER BY r.received_at DESC, n.id LIMIT 500");
        return jdbc.query(sql.toString(), this::sent, args.toArray());
    }

    /** Kết quả từng lần gửi của thông báo (lần chưa có kết quả thì kết quả rỗng). */
    public List<ChannelResult> channels(long notificationId) {
        return jdbc.query(
                "SELECT a.channel, d.result, COALESCE(d.occurred_at, a.scheduled_at) AS at FROM delivery_attempt a"
                        + " LEFT JOIN delivery_result d ON d.attempt_id = a.id WHERE a.notification_id = ?"
                        + " ORDER BY a.id",
                (rs, i) -> new ChannelResult(rs.getString("channel"), rs.getString("result"), instant(rs, "at")),
                notificationId);
    }

    /** Lỗi gửi của một loại, hoặc mọi loại khi {@code type} rỗng; mới trước. */
    public List<DeliveryError> errors(String type) {
        List<DeliveryError> rows = new ArrayList<>();
        String request = "SELECT ? AS type, r.id, NULL AS notification_id, r.event_type, r.source_record_type,"
                + " r.source_record_id, r.event_key, r.rejection_reason AS reason, r.received_at"
                + " FROM notification_request r WHERE ";
        if (type == null || type.equals("REJECTED")) {
            rows.addAll(jdbc.query(request + "r.intake_result = 'REJECTED'", LookupStore::error, "REJECTED"));
        }
        for (String flag : List.of("MISSING_LEVEL", "MISSING_INFORMATION_TYPE", "NO_RECIPIENT")) {
            if (type == null || type.equals(flag)) {
                rows.addAll(jdbc.query(request + "FIND_IN_SET(?, r.flags) > 0", LookupStore::error, flag, flag));
            }
        }
        String notification = "SELECT ? AS type, r.id, n.id AS notification_id, r.event_type, r.source_record_type,"
                + " r.source_record_id, r.event_key, NULL AS reason, r.received_at FROM notification n"
                + " JOIN notification_request r ON r.id = n.request_id";
        if (type == null || type.equals("DELIVERY_FAILED")) {
            rows.addAll(jdbc.query(
                    notification + " JOIN notification_state s ON s.notification_id = n.id"
                            + " WHERE s.status = 'DELIVERY_FAILED'",
                    LookupStore::error,
                    "DELIVERY_FAILED"));
        }
        if (type == null || type.equals("INVALID_CONTENT")) {
            rows.addAll(jdbc.query(
                    notification + " WHERE EXISTS (SELECT 1 FROM delivery_attempt a JOIN delivery_result d"
                            + " ON d.attempt_id = a.id WHERE a.notification_id = n.id AND d.result = 'INVALID_CONTENT')",
                    LookupStore::error,
                    "INVALID_CONTENT"));
        }
        rows.sort((a, b) -> b.receivedAt().compareTo(a.receivedAt()));
        return rows;
    }

    private SentNotification sent(ResultSet rs, int i) throws SQLException {
        String subject = rs.getString("information_subject");
        String q354 = rs.getString("q354_case");
        String status = rs.getString("status");
        String method = rs.getString("acknowledgement_method");
        String flags = rs.getString("flags");
        return new SentNotification(
                rs.getLong("id"),
                rs.getLong("request_id"),
                rs.getString("source_group"),
                rs.getString("source_record_type"),
                rs.getString("source_record_id"),
                NotificationLevel.valueOf(rs.getString("level")),
                RecipientType.valueOf(rs.getString("recipient_type")),
                rs.getLong("recipient_id"),
                json.readValue(rs.getString("basis"), new TypeReference<List<String>>() {}),
                subject == null ? null : InformationSubject.valueOf(subject),
                q354 == null ? null : Q354Case.valueOf(q354),
                instant(rs, "received_at"),
                status == null ? null : NotificationStatus.valueOf(status),
                instant(rs, "sent_at"),
                instant(rs, "viewed_at"),
                instant(rs, "acknowledged_at"),
                method == null ? null : AcknowledgementMethod.valueOf(method),
                flags == null || flags.isBlank() ? List.of() : Arrays.asList(flags.split(",")));
    }

    private static DeliveryError error(ResultSet rs, int i) throws SQLException {
        return new DeliveryError(
                rs.getString("type"),
                rs.getLong("id"),
                rs.getObject("notification_id", Long.class),
                rs.getString("event_type"),
                rs.getString("source_record_type"),
                rs.getString("source_record_id"),
                rs.getString("event_key"),
                rs.getString("reason"),
                instant(rs, "received_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}
