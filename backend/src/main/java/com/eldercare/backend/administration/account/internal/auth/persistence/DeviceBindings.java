package com.eldercare.backend.administration.account.internal.auth.persistence;

import com.eldercare.backend.administration.account.internal.auth.domain.Binding;
import com.eldercare.backend.administration.account.internal.auth.domain.DeviceOwner;
import com.eldercare.backend.administration.account.internal.auth.domain.QrCode;
import com.eldercare.backend.common.time.BusinessTime;
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
 * Mã QR đăng nhập ({@code LOGIN_QR_CODE}) và thiết bị gắn ({@code BOUND_DEVICE}) của tài khoản người cao tuổi ở
 * {@code device_credential} (002 T090, T092, T098; data-model mục 0.1; research A-15). Mã QR và token thiết bị chỉ lưu
 * SHA-256. Chỉ thêm dòng và đặt cột dùng, vô hiệu, gỡ một lần.
 */
@Component
public class DeviceBindings {

    private final JdbcTemplate jdbc;
    private final BusinessTime time;

    DeviceBindings(JdbcTemplate jdbc, BusinessTime time) {
        this.jdbc = jdbc;
        this.time = time;
    }

    /** Vô hiệu mọi mã chưa dùng của tài khoản (mã mới thay mã cũ, FR-069). */
    public void revokeUnusedCodes(long accountId) {
        jdbc.update(
                "UPDATE device_credential SET revoked_at = ? WHERE kind = 'LOGIN_QR_CODE' AND account_id = ?"
                        + " AND used_at IS NULL AND revoked_at IS NULL",
                now(),
                accountId);
    }

    public long insertCode(long accountId, byte[] codeHash, long createdBy, Instant expiresAt) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        LocalDateTime now = now();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO device_credential (kind, account_id, secret_hash, created_by, created_at,"
                                    + " expires_at) VALUES ('LOGIN_QR_CODE', ?, ?, ?, ?, ?)",
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
                                + " FROM device_credential WHERE kind = 'LOGIN_QR_CODE' AND secret_hash = ? FOR UPDATE",
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
        jdbc.update(
                "UPDATE device_credential SET used_at = ?, device_id = ? WHERE id = ? AND kind = 'LOGIN_QR_CODE'",
                now(),
                deviceId,
                codeId);
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
                        "SELECT id, account_id, device_id, created_at FROM device_credential WHERE active_account_id = ?",
                        (rs, i) -> binding(rs),
                        accountId)
                .stream()
                .findFirst();
    }

    public void bind(long accountId, String deviceId, byte[] deviceTokenHash, Long qrCodeId) {
        jdbc.update(
                "INSERT INTO device_credential (kind, account_id, device_id, secret_hash, created_at, login_qr_code_id)"
                        + " VALUES ('BOUND_DEVICE', ?, ?, ?, ?, ?)",
                accountId,
                deviceId,
                deviceTokenHash,
                now(),
                qrCodeId);
    }

    /** Gỡ một lần: {@code NEW_DEVICE_BOUND}, {@code MANUAL} (kèm lý do), {@code ACCOUNT_LOCKED}. */
    public void unbind(long bindingId, String reason, Long actorId, String note) {
        jdbc.update(
                "UPDATE device_credential SET ended_at = ?, end_reason = ?, ended_by = ?, end_note = ?"
                        + " WHERE id = ? AND kind = 'BOUND_DEVICE' AND ended_at IS NULL",
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
                        "SELECT a.resident_id, a.id, b.device_id, a.status FROM device_credential b"
                                + " JOIN account a ON a.id = b.account_id WHERE b.kind = 'BOUND_DEVICE'"
                                + " AND b.secret_hash = ? AND b.ended_at IS NULL AND a.type = 'RESIDENT'",
                        (rs, i) -> new DeviceOwner(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4)),
                        deviceTokenHash)
                .stream()
                .findFirst();
    }

    private Optional<Binding> active(String condition, Object value) {
        return jdbc
                .query(
                        "SELECT id, account_id, device_id, created_at FROM device_credential WHERE " + condition
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
