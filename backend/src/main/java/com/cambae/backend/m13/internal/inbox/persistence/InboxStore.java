package com.cambae.backend.m13.internal.inbox.persistence;

import com.cambae.backend.common.notification.ContentPart;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.m13.api.dto.AcknowledgementMethod;
import com.cambae.backend.m13.api.dto.NotificationStatus;
import com.cambae.backend.m13.api.dto.RecipientType;
import com.cambae.backend.m13.internal.inbox.domain.InboxCursor;
import com.cambae.backend.m13.internal.inbox.domain.InboxFilter;
import com.cambae.backend.m13.internal.inbox.domain.InboxItem;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Đọc hộp thông báo của một người nhận (FR-033, FR-034; research B-11). */
@Repository
public class InboxStore {

    private static final String COLUMNS = "SELECT n.id, n.request_id, n.recipient_type, n.recipient_id, s.level,"
            + " r.event_type, r.title, n.visible_parts, n.identity_only, s.status, s.sent_at, s.viewed_at,"
            + " s.acknowledged_at, s.acknowledgement_method, s.hidden, r.resident_id, r.floor_area_id,"
            + " r.source_record_type, r.source_record_id"
            + " FROM notification_state s JOIN notification n ON n.id = s.notification_id"
            + " JOIN notification_request r ON r.id = n.request_id";

    private static final String URGENT_FIRST = "IF(s.level = 'URGENT' AND s.acknowledged_at IS NULL, 1, 0)";

    private final JdbcTemplate jdbc;

    public InboxStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Thông báo đã gửi, chưa ẩn của người nhận; Khẩn cấp chưa xác nhận trước, rồi mới gửi trước. */
    public List<InboxItem> page(String recipientKey, InboxFilter filter, InboxCursor after, int size) {
        StringBuilder sql = new StringBuilder(
                COLUMNS + " WHERE s.recipient_key = ? AND s.hidden = FALSE AND s.sent_at IS NOT NULL");
        List<Object> args = new ArrayList<>(List.of(recipientKey));
        if (filter.level() != null) {
            sql.append(" AND s.level = ?");
            args.add(filter.level().name());
        }
        if (Boolean.TRUE.equals(filter.unread())) {
            sql.append(" AND s.viewed_at IS NULL");
        }
        if (filter.eventType() != null) {
            sql.append(" AND r.event_type = ?");
            args.add(filter.eventType());
        }
        if (filter.residentId() != null) {
            sql.append(" AND r.resident_id = ?");
            args.add(filter.residentId());
        }
        if (after != null) {
            sql.append(" AND (" + URGENT_FIRST + ", s.sent_at, n.id) < (?, ?, ?)");
            args.add(after.urgentUnacknowledged());
            args.add(utc(after.sentAt()));
            args.add(after.id());
        }
        sql.append(" ORDER BY " + URGENT_FIRST + " DESC, s.sent_at DESC, n.id DESC LIMIT ?");
        args.add(size);
        return jdbc.query(sql.toString(), InboxStore::row, args.toArray());
    }

    public Optional<InboxItem> find(long notificationId, String recipientKey) {
        return jdbc
                .query(
                        COLUMNS + " WHERE n.id = ? AND s.recipient_key = ?",
                        InboxStore::row,
                        notificationId,
                        recipientKey)
                .stream()
                .findFirst();
    }

    /** Số chưa xem và số Khẩn cấp chưa xác nhận trong hộp (không tính thông báo đã ẩn). */
    public int[] counts(String recipientKey) {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(s.viewed_at IS NULL), 0), COALESCE(SUM(s.level = 'URGENT' AND s.acknowledged_at IS NULL), 0)"
                        + " FROM notification_state s WHERE s.recipient_key = ? AND s.hidden = FALSE"
                        + " AND s.sent_at IS NOT NULL",
                (rs, i) -> new int[] {rs.getInt(1), rs.getInt(2)},
                recipientKey);
    }

    /** Các phần nội dung của yêu cầu theo thứ tự. */
    public List<ContentPart> content(long requestId) {
        return jdbc.query(
                "SELECT information_type, content FROM notification_request_content WHERE request_id = ? ORDER BY seq",
                (rs, i) -> new ContentPart(InformationType.valueOf(rs.getString(1)), rs.getString(2)),
                requestId);
    }

    private static InboxItem row(ResultSet rs, int i) throws SQLException {
        String status = rs.getString("status");
        String method = rs.getString("acknowledgement_method");
        return new InboxItem(
                rs.getLong("id"),
                rs.getLong("request_id"),
                RecipientType.valueOf(rs.getString("recipient_type")),
                rs.getLong("recipient_id"),
                NotificationLevel.valueOf(rs.getString("level")),
                rs.getString("event_type"),
                rs.getString("title"),
                parts(rs.getString("visible_parts")),
                rs.getBoolean("identity_only"),
                status == null ? null : NotificationStatus.valueOf(status),
                instant(rs, "sent_at"),
                instant(rs, "viewed_at"),
                instant(rs, "acknowledged_at"),
                method == null ? null : AcknowledgementMethod.valueOf(method),
                rs.getBoolean("hidden"),
                rs.getObject("resident_id", Long.class),
                rs.getObject("floor_area_id", Long.class),
                rs.getString("source_record_type"),
                rs.getString("source_record_id"));
    }

    private static Set<InformationType> parts(String set) {
        Set<InformationType> parts = EnumSet.noneOf(InformationType.class);
        if (set != null && !set.isBlank()) {
            Arrays.stream(set.split(",")).map(InformationType::valueOf).forEach(parts::add);
        }
        return parts;
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}
