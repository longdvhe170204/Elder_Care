package com.cambae.backend.m13.internal.delivery.persistence;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Khung gộp tin nhắn Trung bình ({@code khung_gop_tin_nhan}; FR-016b, data-model mục 6). */
@Repository
public class SmsWindowStore {

    public record Window(long id, String recipientKey, Instant endsAt) {}

    /** Tin dồn trong khung, chưa có kết quả. */
    public record Batched(long attemptId, long notificationId, String address) {}

    private final JdbcTemplate jdbc;

    SmsWindowStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Khung đang mở của người nhận, khóa dòng. */
    public Optional<Window> openFor(String recipientKey) {
        return jdbc
                .query(
                        "SELECT id, recipient_key, ends_at FROM sms_batch_window WHERE open_key = ? FOR UPDATE",
                        (rs, i) -> new Window(
                                rs.getLong(1), rs.getString(2), instant(rs.getObject(3, LocalDateTime.class))),
                        recipientKey)
                .stream()
                .findFirst();
    }

    /** Mở khung; người nhận đã có khung mở thì ném {@code DuplicateKeyException}. */
    public long open(String recipientKey, Instant startedAt, Instant endsAt, long firstAttemptId) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO sms_batch_window (recipient_key, started_at, ends_at, first_attempt_id)"
                                    + " VALUES (?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, recipientKey);
                    ps.setObject(2, utc(startedAt));
                    ps.setObject(3, utc(endsAt));
                    ps.setLong(4, firstAttemptId);
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    /** Id khung mở đã hết hạn. */
    public List<Long> due(Instant now) {
        return jdbc.queryForList(
                "SELECT id FROM sms_batch_window WHERE closed_at IS NULL AND ends_at <= ? ORDER BY ends_at",
                Long.class,
                utc(now));
    }

    /** Khóa và đọc khung còn mở. */
    public Optional<Window> lockOpen(long windowId) {
        return jdbc
                .query(
                        "SELECT id, recipient_key, ends_at FROM sms_batch_window WHERE id = ? AND closed_at IS NULL"
                                + " FOR UPDATE",
                        (rs, i) -> new Window(
                                rs.getLong(1), rs.getString(2), instant(rs.getObject(3, LocalDateTime.class))),
                        windowId)
                .stream()
                .findFirst();
    }

    public List<Batched> batched(long windowId) {
        return jdbc.query(
                "SELECT a.id, a.notification_id, a.address FROM delivery_attempt a WHERE a.batch_window_id = ?"
                        + " AND NOT EXISTS (SELECT 1 FROM delivery_result r WHERE r.attempt_id = a.id) ORDER BY a.id",
                (rs, i) -> new Batched(rs.getLong(1), rs.getLong(2), rs.getString(3)),
                windowId);
    }

    public void close(long windowId, Instant at, Long digestAttemptId) {
        jdbc.update(
                "UPDATE sms_batch_window SET closed_at = ?, digest_attempt_id = ? WHERE id = ?",
                utc(at),
                digestAttemptId,
                windowId);
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }

    private static Instant instant(LocalDateTime at) {
        return at.toInstant(ZoneOffset.UTC);
    }
}
