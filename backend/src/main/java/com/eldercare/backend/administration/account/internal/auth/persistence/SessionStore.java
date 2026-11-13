package com.eldercare.backend.administration.account.internal.auth.persistence;

import com.eldercare.backend.administration.account.api.dto.AccessChannel;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.internal.auth.domain.SessionRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Đọc, ghi {@code login_session} theo token và lần kết nối của thiết bị ở {@code device} (002 research A-02; FR-013,
 * FR-013a, FR-041a): tìm theo khóa chính {@code token_hash}, không đệm giữa các yêu cầu.
 */
@Component
public class SessionStore {

    private static final String SELECT = "SELECT s.account_id, s.device_id, s.channel, s.last_active_at, a.type,"
            + " a.staff_id, a.family_member_id, a.must_change_password, a.resident_id, s.ended_at IS NULL"
            + " FROM login_session s JOIN account a ON a.id = s.account_id WHERE s.token_hash = ?";

    private static final String DEVICE_LOCK = "M15.DEVICE:";

    private final JdbcTemplate jdbc;

    SessionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Phiên đang mở của token. */
    public Optional<SessionRow> open(byte[] tokenHash) {
        return jdbc.query(SELECT + " AND s.ended_at IS NULL", SessionStore::row, tokenHash).stream()
                .findFirst();
    }

    /** Phiên của token, kể cả đã kết thúc, trừ đăng xuất (đồng bộ ngoại tuyến, FR-041a). */
    public Optional<SessionRow> forSync(byte[] tokenHash) {
        return jdbc
                .query(SELECT + " AND (s.end_reason IS NULL OR s.end_reason <> 'LOGOUT')", SessionStore::row, tokenHash)
                .stream()
                .findFirst();
    }

    /** Tài khoản còn phiên đang mở trên thiết bị. */
    public boolean openOnDevice(long accountId, String deviceId) {
        Integer open = jdbc.queryForObject(
                "SELECT COUNT(*) FROM login_session WHERE account_id = ? AND device_id = ? AND ended_at IS NULL",
                Integer.class,
                accountId,
                deviceId);
        return open != null && open > 0;
    }

    /** Kết thúc phiên đang mở vì quá thời gian không thao tác (CFG-M15-10). */
    public void timeOut(byte[] tokenHash, Instant now) {
        jdbc.update(
                "UPDATE login_session SET ended_at = ?, end_reason = 'TIMED_OUT' WHERE token_hash = ? AND ended_at IS NULL",
                utc(now),
                tokenHash);
    }

    /** Đếm lại thời gian không thao tác của phiên đang mở. */
    public void touch(byte[] tokenHash, Instant now) {
        jdbc.update(
                "UPDATE login_session SET last_active_at = ? WHERE token_hash = ? AND ended_at IS NULL",
                utc(now),
                tokenHash);
    }

    /**
     * Lần kết nối gần nhất của thiết bị, khóa dòng tới hết transaction (đối chiếu đồng bộ ngoại tuyến). Thiết bị là dòng
     * {@code technical_lock} khóa {@code M15.DEVICE:<id thiết bị>}, {@code updated_at} là lần kết nối gần nhất (gộp bảng
     * {@code device} 2026-11-13, research R-15 của 000).
     */
    public Optional<Instant> lockLastConnection(String deviceId) {
        return jdbc
                .queryForList(
                        "SELECT updated_at FROM technical_lock WHERE lock_key = ? FOR UPDATE",
                        LocalDateTime.class,
                        DEVICE_LOCK + deviceId)
                .stream()
                .findFirst()
                .map(t -> t.toInstant(ZoneOffset.UTC));
    }

    /** Ghi lần kết nối của thiết bị. */
    public void recordConnection(String deviceId, Instant at, long accountId) {
        jdbc.update(
                "INSERT INTO technical_lock (lock_key, version, updated_at) VALUES (?, 0, ?) AS new"
                        + " ON DUPLICATE KEY UPDATE updated_at = new.updated_at",
                DEVICE_LOCK + deviceId,
                utc(at));
    }

    private static SessionRow row(ResultSet rs, int i) throws SQLException {
        return new SessionRow(
                rs.getLong(1),
                rs.getString(2),
                AccessChannel.valueOf(rs.getString(3)),
                rs.getObject(4, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                AccountType.valueOf(rs.getString(5)),
                Optional.ofNullable(rs.getObject(6, Long.class)),
                Optional.ofNullable(rs.getObject(7, Long.class)),
                rs.getBoolean(8),
                Optional.ofNullable(rs.getObject(9, Long.class)),
                rs.getBoolean(10));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
