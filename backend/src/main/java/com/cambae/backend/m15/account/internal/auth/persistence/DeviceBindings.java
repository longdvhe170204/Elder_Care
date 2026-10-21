package com.cambae.backend.m15.account.internal.auth.persistence;

import com.cambae.backend.common.time.BusinessTime;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

/**
 * Bảng {@code login_qr_code}, {@code bound_device} của tài khoản người cao tuổi (002 T090, T092; data-model mục 2;
 * research A-15). Mã QR và token thiết bị chỉ lưu SHA-256. Chỉ thêm dòng và đặt cột dùng, vô hiệu, gỡ một lần.
 */
@Component
public class DeviceBindings {

    private final JdbcTemplate jdbc;
    private final BusinessTime time;

    DeviceBindings(JdbcTemplate jdbc, BusinessTime time) {
        this.jdbc = jdbc;
        this.time = time;
    }

    /** Một mã QR; dùng được khi chưa dùng, chưa vô hiệu và {@code expiresAt} sau thời điểm kiểm (FR-069). */
    public record QrCode(long id, long accountId, Instant expiresAt, boolean used, boolean revoked) {

        public boolean usableAt(Instant now) {
            return !used && !revoked && expiresAt.isAfter(now);
        }
    }

    /** Thiết bị đang gắn của một tài khoản. */
    public record Binding(long id, long accountId, String deviceId, Instant boundAt) {}

    /** Kết quả tra token thiết bị: người cao tuổi, tài khoản, thiết bị và trạng thái tài khoản lúc tra. */
    public record DeviceOwner(long residentId, long accountId, String deviceId, String accountStatus) {}

    /** Vô hiệu mọi mã chưa dùng của tài khoản (mã mới thay mã cũ, FR-069). */
    public void revokeUnusedCodes(long accountId) {
        jdbc.update(
                "UPDATE login_qr_code SET revoked_at = ? WHERE account_id = ? AND used_at IS NULL AND revoked_at IS NULL",
                now(),
                accountId);
    }

    public long insertCode(long accountId, byte[] codeHash, long createdBy, Instant expiresAt) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        LocalDateTime now = now();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO login_qr_code (account_id, code_hash, created_by, created_at, expires_at)"
                                    + " VALUES (?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, accountId);
                    ps.setBytes(2, codeHash);
                    ps.setLong(3, createdBy);
                    ps.setObject(4, now);
                    ps.setObject(5, utc(expiresAt));
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    /** Mã theo băm, khóa dòng tới hết transaction đăng nhập. */
    public Optional<QrCode> lockCode(byte[] codeHash) {
        return jdbc
                .query(
                        "SELECT id, account_id, expires_at, used_at IS NOT NULL, revoked_at IS NOT NULL"
                                + " FROM login_qr_code WHERE code_hash = ? FOR UPDATE",
                        (rs, i) -> new QrCode(
                                rs.getLong(1),
                                rs.getLong(2),
                                rs.getObject(3, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                                rs.getBoolean(4),
                                rs.getBoolean(5)),
                        codeHash)
                .stream()
                .findFirst();
    }

    public void markCodeUsed(long codeId, String deviceId) {
        jdbc.update("UPDATE login_qr_code SET used_at = ?, used_device_id = ? WHERE id = ?", now(), deviceId, codeId);
    }

    /** Thiết bị đang gắn của tài khoản, khóa dòng. */
    public Optional<Binding> lockActiveOfAccount(long accountId) {
        return active("active_account_id = ?", accountId);
    }

    /** Tài khoản đang gắn với thiết bị, khóa dòng (FR-014a). */
    public Optional<Binding> lockActiveOfDevice(String deviceId) {
        return active("active_device_id = ?", deviceId);
    }

    /** Thiết bị đang gắn của tài khoản, không khóa (hiển thị, {@code allowedCommands}). */
    public Optional<Binding> activeOfAccount(long accountId) {
        return jdbc
                .query(
                        "SELECT id, account_id, device_id, bound_at FROM bound_device WHERE active_account_id = ?",
                        (rs, i) -> binding(rs),
                        accountId)
                .stream()
                .findFirst();
    }

    public void bind(long accountId, String deviceId, byte[] deviceTokenHash, Long qrCodeId) {
        jdbc.update(
                "INSERT INTO bound_device (account_id, device_id, device_token_hash, bound_at, login_qr_code_id)"
                        + " VALUES (?, ?, ?, ?, ?)",
                accountId,
                deviceId,
                deviceTokenHash,
                now(),
                qrCodeId);
    }

    /** Gỡ một lần: {@code NEW_DEVICE_BOUND}, {@code MANUAL} (kèm lý do), {@code ACCOUNT_LOCKED}. */
    public void unbind(long bindingId, String reason, Long actorId, String note) {
        jdbc.update(
                "UPDATE bound_device SET unbound_at = ?, unbind_reason = ?, unbound_by = ?, unbind_note = ?"
                        + " WHERE id = ? AND unbound_at IS NULL",
                now(),
                reason,
                actorId,
                note,
                bindingId);
    }

    /** Kết thúc phiên đang mở của tài khoản trên một thiết bị (gỡ thiết bị, FR-070). */
    public void endSessionsOnDevice(long accountId, String deviceId, String reason) {
        jdbc.update(
                "UPDATE login_session SET ended_at = ?, end_reason = ? WHERE account_id = ? AND device_id = ?"
                        + " AND ended_at IS NULL",
                now(),
                reason,
                accountId,
                deviceId);
    }

    /** Chủ của token thiết bị đang gắn (FR-071); rỗng khi không có thiết bị đang gắn với băm này. */
    public Optional<DeviceOwner> owner(byte[] deviceTokenHash) {
        return jdbc
                .query(
                        "SELECT a.resident_id, a.id, b.device_id, a.status FROM bound_device b"
                                + " JOIN account a ON a.id = b.account_id"
                                + " WHERE b.device_token_hash = ? AND b.unbound_at IS NULL AND a.type = 'RESIDENT'",
                        (rs, i) -> new DeviceOwner(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4)),
                        deviceTokenHash)
                .stream()
                .findFirst();
    }

    private Optional<Binding> active(String condition, Object value) {
        return jdbc
                .query(
                        "SELECT id, account_id, device_id, bound_at FROM bound_device WHERE " + condition
                                + " FOR UPDATE",
                        (rs, i) -> binding(rs),
                        value)
                .stream()
                .findFirst();
    }

    private static Binding binding(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Binding(
                rs.getLong(1),
                rs.getLong(2),
                rs.getString(3),
                rs.getObject(4, LocalDateTime.class).toInstant(ZoneOffset.UTC));
    }

    private LocalDateTime now() {
        return utc(time.now());
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
