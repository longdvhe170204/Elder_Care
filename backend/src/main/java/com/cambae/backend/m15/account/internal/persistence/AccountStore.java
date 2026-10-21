package com.cambae.backend.m15.account.internal.persistence;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.dto.AccountStatus;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.internal.domain.Account;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

/**
 * Đọc, ghi {@code account}, {@code account_status_history}, {@code role_assignment}, {@code login_session} cho lệnh tài
 * khoản (002 T030, T031; data-model mục 2, 3). JDBC như phần còn lại của M15; không có entity JPA (plan 002 ghi chú T030).
 */
@Component
public class AccountStore {

    private static final String COLUMNS = "id, username, type, staff_id, family_member_id, status, lock_cause,"
            + " locked_until, last_login_at, version, permission_version";

    private final JdbcTemplate jdbc;
    private final BusinessTime time;

    AccountStore(JdbcTemplate jdbc, BusinessTime time) {
        this.jdbc = jdbc;
        this.time = time;
    }

    public Optional<Account> find(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM account WHERE id = ?", AccountStore::row, id).stream()
                .findFirst();
    }

    /** Khóa dòng cho lệnh trạng thái, phân quyền (kiểm lại trên trạng thái mới, 000 FR-040). */
    public Optional<Account> lock(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM account WHERE id = ? FOR UPDATE", AccountStore::row, id).stream()
                .findFirst();
    }

    public Optional<Account> byStaff(long staffId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM account WHERE staff_id = ?", AccountStore::row, staffId).stream()
                .findFirst();
    }

    public Optional<Account> byFamilyMember(long familyMemberId) {
        return jdbc
                .query(
                        "SELECT " + COLUMNS + " FROM account WHERE family_member_id = ?",
                        AccountStore::row,
                        familyMemberId)
                .stream()
                .findFirst();
    }

    /** Trang theo id tăng dần; {@code afterId} là con trỏ trang trước. */
    public List<Account> page(AccountType type, AccountStatus status, String q, Long afterId, int size) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM account WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (type != null) {
            sql.append(" AND type = ?");
            args.add(type.name());
        }
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status.name());
        }
        if (q != null && !q.isBlank()) {
            sql.append(" AND username LIKE ?");
            args.add("%" + q.strip() + "%");
        }
        if (afterId != null) {
            sql.append(" AND id > ?");
            args.add(afterId);
        }
        sql.append(" ORDER BY id LIMIT ?");
        args.add(size);
        return jdbc.query(sql.toString(), AccountStore::row, args.toArray());
    }

    /** Vai trò đang gán (còn hiệu lực lúc này). */
    public Set<Role> roles(long accountId) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        jdbc.query(
                "SELECT role FROM role_assignment WHERE account_id = ? AND effective_to IS NULL AND effective_from <= ?",
                rs -> {
                    roles.add(role(rs.getString(1)));
                },
                accountId,
                utc(time.now()));
        return roles;
    }

    /** Có quyền Duyệt kế hoạch chăm sóc gán thêm đang hiệu lực (FR-024; cho {@code allowedCommands}). */
    public boolean hasCarePlanApprovalGrant(long accountId) {
        LocalDateTime now = utc(time.now());
        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM additional_grant WHERE account_id = ? AND effective_from <= ?"
                        + " AND (effective_to IS NULL OR effective_to > ?)",
                Integer.class,
                accountId,
                now,
                now);
        return rows != null && rows > 0;
    }

    /** Vai trò gán tại {@code at} ({@code effective_from <= at < effective_to}; FR-026a). */
    public Set<Role> rolesAt(long accountId, Instant at) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        jdbc.query(
                "SELECT role FROM role_assignment WHERE account_id = ? AND effective_from <= ?"
                        + " AND (effective_to IS NULL OR effective_to > ?)",
                rs -> {
                    roles.add(role(rs.getString(1)));
                },
                accountId,
                utc(at),
                utc(at));
        return roles;
    }

    /**
     * Trạng thái tại {@code at} theo {@code account_status_history} (FR-026a): dòng gần nhất không sau {@code at}; chưa
     * có dòng nào trước {@code at} mà có dòng sau thì Hoạt động (tài khoản tạo ra ở trạng thái Hoạt động); không có
     * lịch sử (dữ liệu trước T031) thì trạng thái hiện tại.
     */
    public AccountStatus statusAt(long accountId, Instant at, AccountStatus current) {
        List<String> rows = jdbc.queryForList(
                "SELECT status FROM account_status_history WHERE account_id = ? AND effective_from <= ?"
                        + " ORDER BY effective_from DESC LIMIT 1",
                String.class,
                accountId,
                utc(at));
        if (!rows.isEmpty()) {
            return AccountStatus.valueOf(rows.getFirst());
        }
        Integer later = jdbc.queryForObject(
                "SELECT COUNT(*) FROM account_status_history WHERE account_id = ?", Integer.class, accountId);
        return later != null && later > 0 ? AccountStatus.ACTIVE : current;
    }

    /**
     * Số tài khoản Hoạt động khác {@code excludedAccountId} đang có vai trò Quản lý viện; khóa các dòng đó để hai lệnh
     * đồng thời không cùng thấy "còn người khác" (FR-012; research A-05).
     */
    public int otherActiveManagers(long excludedAccountId) {
        return jdbc.queryForList(
                        "SELECT ra.account_id FROM role_assignment ra JOIN account a ON a.id = ra.account_id"
                                + " WHERE ra.role = 'AC-01' AND ra.effective_to IS NULL AND a.status = 'ACTIVE'"
                                + " AND ra.account_id <> ? FOR UPDATE",
                        Long.class,
                        excludedAccountId)
                .size();
    }

    public long insertStaff(long staffId, String username, String passwordHash, Long createdBy) {
        return insert("staff_id", "STAFF", staffId, username, passwordHash, createdBy);
    }

    /** Tài khoản người thân (FR-050), buộc đổi mật khẩu lần đầu như tài khoản nhân viên. */
    public long insertFamily(long familyMemberId, String username, String passwordHash, Long createdBy) {
        return insert("family_member_id", "FAMILY_MEMBER", familyMemberId, username, passwordHash, createdBy);
    }

    private long insert(
            String linkColumn, String type, long linkId, String username, String passwordHash, Long createdBy) {
        LocalDateTime now = utc(time.now());
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO account (username, type, " + linkColumn + ", status, password_hash,"
                                    + " must_change_password, inactivity_reference_at, created_at, created_by)"
                                    + " VALUES (?, '" + type + "', ?, 'ACTIVE', ?, TRUE, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, username);
                    ps.setLong(2, linkId);
                    ps.setString(3, passwordHash);
                    ps.setObject(4, now);
                    ps.setObject(5, now);
                    ps.setObject(6, createdBy);
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    /** Chuyển trạng thái, tăng {@code version}, thêm một dòng lịch sử (data-model mục 2). */
    public void changeStatus(long accountId, AccountStatus status, String lockCause, Long actorId) {
        LocalDateTime now = utc(time.now());
        jdbc.update(
                "UPDATE account SET status = ?, lock_cause = ?, locked_until = NULL, version = version + 1,"
                        + " inactivity_reference_at = IF(? = 'ACTIVE', ?, inactivity_reference_at) WHERE id = ?",
                status.name(),
                lockCause,
                status.name(),
                now,
                accountId);
        history(accountId, status, lockCause, actorId);
    }

    public void history(long accountId, AccountStatus status, String lockCause, Long actorId) {
        jdbc.update(
                "INSERT INTO account_status_history (account_id, status, lock_cause, effective_from, actor_id)"
                        + " VALUES (?, ?, ?, ?, ?)",
                accountId,
                status.name(),
                lockCause,
                utc(time.now()),
                actorId);
    }

    /** Kết thúc mọi phiên đang mở của tài khoản trong transaction của lệnh (FR-013). */
    public void endSessions(long accountId, String reason) {
        jdbc.update(
                "UPDATE login_session SET ended_at = ?, end_reason = ? WHERE account_id = ? AND ended_at IS NULL",
                utc(time.now()),
                reason,
                accountId);
    }

    public void resetPassword(long accountId, String passwordHash) {
        jdbc.update(
                "UPDATE account SET password_hash = ?, must_change_password = TRUE, failed_login_count = 0 WHERE id = ?",
                passwordHash,
                accountId);
    }

    public void addRole(long accountId, Role role, Long assignedBy, String reason) {
        jdbc.update(
                "INSERT INTO role_assignment (account_id, role, effective_from, assigned_by, reason) VALUES (?, ?, ?, ?, ?)",
                accountId,
                role.code(),
                utc(time.now()),
                assignedBy,
                reason);
    }

    /** Đóng dòng gán vai trò đang hiệu lực; trả số dòng đã đóng. */
    public int closeRole(long accountId, Role role) {
        return jdbc.update(
                "UPDATE role_assignment SET effective_to = ? WHERE account_id = ? AND role = ? AND effective_to IS NULL",
                utc(time.now()),
                accountId,
                role.code());
    }

    public void bumpPermissionVersion(long accountId) {
        jdbc.update("UPDATE account SET permission_version = permission_version + 1 WHERE id = ?", accountId);
    }

    // --- Đăng nhập, mật khẩu (US3) ---------------------------------------------------------------------------

    /** Khóa dòng theo tên đăng nhập cho lệnh đăng nhập (đếm sai, chuyển khóa tạm trong cùng transaction). */
    public Optional<Account> lockByUsername(String username) {
        return jdbc
                .query("SELECT " + COLUMNS + " FROM account WHERE username = ? FOR UPDATE", AccountStore::row, username)
                .stream()
                .findFirst();
    }

    public String passwordHash(long accountId) {
        return jdbc.queryForObject("SELECT password_hash FROM account WHERE id = ?", String.class, accountId);
    }

    public boolean mustChangePassword(long accountId) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject("SELECT must_change_password FROM account WHERE id = ?", Boolean.class, accountId));
    }

    /** Tăng bộ đếm sai bằng một câu lệnh (research A-03); trả giá trị mới. */
    public int recordFailedLogin(long accountId) {
        jdbc.update("UPDATE account SET failed_login_count = failed_login_count + 1 WHERE id = ?", accountId);
        return jdbc.queryForObject("SELECT failed_login_count FROM account WHERE id = ?", Integer.class, accountId);
    }

    public void resetFailedLogins(long accountId) {
        jdbc.update("UPDATE account SET failed_login_count = 0 WHERE id = ?", accountId);
    }

    /** Chuyển Khóa tạm tới {@code until} (FR-008), tăng {@code version}, thêm dòng lịch sử; Bộ lập lịch thực hiện. */
    public void temporarilyLock(long accountId, Instant until) {
        jdbc.update(
                "UPDATE account SET status = 'TEMPORARILY_LOCKED', lock_cause = NULL, locked_until = ?,"
                        + " version = version + 1 WHERE id = ?",
                utc(until),
                accountId);
        history(accountId, AccountStatus.TEMPORARILY_LOCKED, null, null);
    }

    /** Đăng nhập thành công: bộ đếm về 0, lưu lần đăng nhập, đặt lại mốc tính Không hoạt động (FR-008, FR-010). */
    public void recordLogin(long accountId) {
        LocalDateTime now = utc(time.now());
        jdbc.update(
                "UPDATE account SET failed_login_count = 0, last_login_at = ?, inactivity_reference_at = ? WHERE id = ?",
                now,
                now,
                accountId);
    }

    public void changePassword(long accountId, String passwordHash) {
        jdbc.update(
                "UPDATE account SET password_hash = ?, must_change_password = FALSE WHERE id = ?",
                passwordHash,
                accountId);
    }

    /** Kích hoạt lại thì buộc đổi mật khẩu (bảng trạng thái FR-007). */
    public void requirePasswordChange(long accountId) {
        jdbc.update("UPDATE account SET must_change_password = TRUE WHERE id = ?", accountId);
    }

    /** Mở phiên; kết thúc trước mọi phiên đang mở trên thiết bị, của bất kỳ tài khoản nào (FR-014a, BR-M15-15). */
    public void openSession(byte[] tokenHash, long accountId, String deviceId, String channel) {
        LocalDateTime now = utc(time.now());
        jdbc.update(
                "UPDATE login_session SET ended_at = ?, end_reason = 'DEVICE_SIGNED_IN_ELSEWHERE'"
                        + " WHERE device_id = ? AND ended_at IS NULL",
                now,
                deviceId);
        jdbc.update(
                "INSERT INTO login_session (token_hash, account_id, device_id, channel, started_at, last_active_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                tokenHash,
                accountId,
                deviceId,
                channel,
                now,
                now);
    }

    public void endSession(byte[] tokenHash, String reason) {
        jdbc.update(
                "UPDATE login_session SET ended_at = ?, end_reason = ? WHERE token_hash = ? AND ended_at IS NULL",
                utc(time.now()),
                reason,
                tokenHash);
    }

    /** Tài khoản người thân Hoạt động có mốc tính trước {@code before} (FR-010; job Không hoạt động). */
    public List<Long> familyActiveSince(Instant before) {
        return jdbc.queryForList(
                "SELECT id FROM account WHERE type = 'FAMILY_MEMBER' AND status = 'ACTIVE'"
                        + " AND inactivity_reference_at < ? ORDER BY id",
                Long.class,
                utc(before));
    }

    /** Tài khoản người thân chưa Đã khóa (job khóa sau kết thúc lưu trú, FR-049). */
    public List<Long> familyNotLocked() {
        return jdbc.queryForList(
                "SELECT id FROM account WHERE type = 'FAMILY_MEMBER' AND status <> 'LOCKED' ORDER BY id", Long.class);
    }

    public Instant inactivityReference(long accountId) {
        return jdbc.queryForObject(
                        "SELECT inactivity_reference_at FROM account WHERE id = ?", LocalDateTime.class, accountId)
                .toInstant(ZoneOffset.UTC);
    }

    static Role role(String code) {
        for (Role role : Role.values()) {
            if (role.code().equals(code)) {
                return role;
            }
        }
        throw new IllegalStateException("Vai trò không có trong common.Role: " + code);
    }

    private static Account row(ResultSet rs, int i) throws SQLException {
        return new Account(
                rs.getLong("id"),
                rs.getString("username"),
                AccountType.valueOf(rs.getString("type")),
                rs.getObject("staff_id", Long.class),
                rs.getObject("family_member_id", Long.class),
                AccountStatus.valueOf(rs.getString("status")),
                rs.getString("lock_cause"),
                instant(rs, "locked_until"),
                instant(rs, "last_login_at"),
                rs.getInt("version"),
                rs.getInt("permission_version"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
