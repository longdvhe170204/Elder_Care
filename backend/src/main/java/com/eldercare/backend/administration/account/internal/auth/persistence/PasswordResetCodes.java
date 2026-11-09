package com.eldercare.backend.administration.account.internal.auth.persistence;

import com.eldercare.backend.administration.account.internal.auth.domain.PasswordResetCode;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Mã đặt lại mật khẩu ở {@code password_reset_code} (002 T105; FR-052a). Chỉ lưu băm; mỗi mã dùng hoặc hủy một lần. */
@Component
public class PasswordResetCodes {

    private final JdbcTemplate jdbc;
    private final BusinessTime time;

    PasswordResetCodes(JdbcTemplate jdbc, BusinessTime time) {
        this.jdbc = jdbc;
        this.time = time;
    }

    /** Số mã đã gửi từ {@code since} (đầu ngày) và lần gửi gần nhất, cho CFG-M15-14. */
    public int sentSince(long accountId, Instant since) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM password_reset_code WHERE account_id = ? AND requested_at >= ?",
                Integer.class,
                accountId,
                utc(since));
    }

    public Optional<Instant> lastSentAt(long accountId) {
        LocalDateTime last = jdbc.queryForObject(
                "SELECT MAX(requested_at) FROM password_reset_code WHERE account_id = ?",
                (rs, i) -> rs.getObject(1, LocalDateTime.class),
                accountId);
        return Optional.ofNullable(last).map(t -> t.toInstant(ZoneOffset.UTC));
    }

    /** Mã mới thay mọi mã chưa dùng của tài khoản. */
    public void insert(long accountId, byte[] codeHash, Instant expiresAt) {
        LocalDateTime now = utc(time.now());
        jdbc.update(
                "UPDATE password_reset_code SET revoked_at = ? WHERE account_id = ? AND used_at IS NULL"
                        + " AND revoked_at IS NULL",
                now,
                accountId);
        jdbc.update(
                "INSERT INTO password_reset_code (account_id, code_hash, requested_at, expires_at) VALUES (?, ?, ?, ?)",
                accountId,
                codeHash,
                now,
                utc(expiresAt));
    }

    /** Mã mới nhất chưa dùng, chưa hủy, chưa hết hạn; khóa dòng tới hết transaction. */
    public Optional<PasswordResetCode> lockActive(long accountId) {
        return jdbc
                .query(
                        "SELECT id, code_hash, failed_attempts FROM password_reset_code WHERE account_id = ?"
                                + " AND used_at IS NULL AND revoked_at IS NULL AND expires_at > ?"
                                + " ORDER BY requested_at DESC, id DESC LIMIT 1 FOR UPDATE",
                        (rs, i) -> new PasswordResetCode(rs.getLong(1), rs.getBytes(2), rs.getInt(3)),
                        accountId,
                        utc(time.now()))
                .stream()
                .findFirst();
    }

    /** Thêm một lần sai; {@code revoke} thì hủy mã. */
    public void recordFailure(long codeId, boolean revoke) {
        jdbc.update(
                "UPDATE password_reset_code SET failed_attempts = failed_attempts + 1,"
                        + " revoked_at = IF(?, ?, revoked_at) WHERE id = ?",
                revoke,
                utc(time.now()),
                codeId);
    }

    public void markUsed(long codeId) {
        jdbc.update("UPDATE password_reset_code SET used_at = ? WHERE id = ?", utc(time.now()), codeId);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
