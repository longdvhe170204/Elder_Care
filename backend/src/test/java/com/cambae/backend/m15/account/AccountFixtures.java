package com.cambae.backend.m15.account;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.dto.AccessChannel;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.internal.auth.domain.ActiveSession;
import com.cambae.backend.m15.account.internal.auth.service.SessionTokens;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;

/** Dựng tài khoản nhân viên, vai trò, phiên trực tiếp trong cơ sở dữ liệu cho test của 002 (US2 trở đi). */
class AccountFixtures {

    private static final AtomicLong IDS = new AtomicLong(1_000_000);

    private final JdbcTemplate jdbc;
    private final BusinessTime time;

    AccountFixtures(JdbcTemplate jdbc, BusinessTime time) {
        this.jdbc = jdbc;
        this.time = time;
    }

    static long nextId() {
        return IDS.incrementAndGet();
    }

    /** Tài khoản nhân viên Hoạt động có các vai trò đã cho (mã AC-xx); trả id tài khoản. */
    long staffAccount(String... roles) {
        long staffId = nextId();
        LocalDateTime created = utc(time.now().minus(Duration.ofDays(1)));
        jdbc.update("""
                INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password,
                                     inactivity_reference_at, created_at)
                VALUES (?, 'STAFF', ?, 'ACTIVE', 'x', FALSE, ?, ?)
                """, "fixture-" + staffId, staffId, created, created);
        long accountId = jdbc.queryForObject("SELECT id FROM account WHERE staff_id = ?", Long.class, staffId);
        for (String role : roles) {
            jdbc.update(
                    "INSERT INTO role_assignment (account_id, role, effective_from, reason) VALUES (?, ?, ?, 'Test')",
                    accountId,
                    role,
                    created);
        }
        return accountId;
    }

    /** Đặt mật khẩu (băm Argon2id như bản thật) cho tài khoản fixture; trả tên đăng nhập. */
    String password(
            long accountId, String password, org.springframework.security.crypto.password.PasswordEncoder encoder) {
        jdbc.update("UPDATE account SET password_hash = ? WHERE id = ?", encoder.encode(password), accountId);
        return jdbc.queryForObject("SELECT username FROM account WHERE id = ?", String.class, accountId);
    }

    /** Tài khoản người thân Hoạt động (vai trò AC-10); trả id tài khoản. */
    long familyAccount(long familyMemberId) {
        LocalDateTime created = utc(time.now().minus(Duration.ofDays(1)));
        jdbc.update("""
                INSERT INTO account (username, type, family_member_id, status, password_hash, must_change_password,
                                     inactivity_reference_at, created_at)
                VALUES (?, 'FAMILY_MEMBER', ?, 'ACTIVE', 'x', FALSE, ?, ?)
                """, "family-" + familyMemberId, familyMemberId, created, created);
        long accountId =
                jdbc.queryForObject("SELECT id FROM account WHERE family_member_id = ?", Long.class, familyMemberId);
        jdbc.update(
                "INSERT INTO role_assignment (account_id, role, effective_from, reason) VALUES (?, 'AC-10', ?, 'Test')",
                accountId,
                created);
        return accountId;
    }

    /**
     * Tài khoản người cao tuổi Hoạt động (vai trò AC-16, 002 US8) dựng thẳng trong cơ sở dữ liệu, không qua lệnh tạo
     * (T091 chờ {@code NguoiCaoTuoiApi} của 001); trả id tài khoản.
     */
    long residentAccount(long residentId) {
        LocalDateTime created = utc(time.now().minus(Duration.ofDays(1)));
        jdbc.update("""
                INSERT INTO account (username, type, resident_id, status, password_hash, must_change_password,
                                     inactivity_reference_at, created_at)
                VALUES (?, 'RESIDENT', ?, 'ACTIVE', 'x', FALSE, ?, ?)
                """, "resident-" + residentId, residentId, created, created);
        long accountId = jdbc.queryForObject("SELECT id FROM account WHERE resident_id = ?", Long.class, residentId);
        jdbc.update(
                "INSERT INTO role_assignment (account_id, role, effective_from, reason) VALUES (?, 'AC-16', ?, 'Test')",
                accountId,
                created);
        return accountId;
    }

    /** {@code POST /auth/qr-login} trên thiết bị đã cho (002 US8). */
    static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder qrLogin(
            String code, String deviceId) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/qr-login")
                .header("X-Device-Id", deviceId)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}");
    }

    /** Người dùng của tài khoản nhân viên fixture với vai trò đang gán, như phiên app nhân viên (002 US4 trở đi). */
    ActiveSession user(long accountId) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (String code : jdbc.queryForList(
                "SELECT role FROM role_assignment WHERE account_id = ? AND effective_to IS NULL",
                String.class,
                accountId)) {
            Arrays.stream(Role.values()).filter(r -> r.code().equals(code)).forEach(roles::add);
        }
        return new ActiveSession(
                accountId,
                AccountType.STAFF,
                Optional.of(staffId(accountId)),
                Optional.empty(),
                roles,
                "fixture-device",
                AccessChannel.STAFF_APP);
    }

    /** Người dùng của tài khoản người thân fixture, như phiên app người thân (002 US6). */
    ActiveSession familyUser(long accountId) {
        long familyMemberId =
                jdbc.queryForObject("SELECT family_member_id FROM account WHERE id = ?", Long.class, accountId);
        return new ActiveSession(
                accountId,
                AccountType.FAMILY_MEMBER,
                Optional.empty(),
                Optional.of(familyMemberId),
                EnumSet.of(Role.FAMILY_MEMBER),
                "fixture-device",
                AccessChannel.FAMILY_APP);
    }

    long staffId(long accountId) {
        return jdbc.queryForObject("SELECT staff_id FROM account WHERE id = ?", Long.class, accountId);
    }

    /** Mở phiên kênh app nhân viên; trả token để gửi bằng {@code Authorization: Bearer}. */
    String session(long accountId) {
        return session(accountId, UUID.randomUUID().toString());
    }

    /** Như {@link #session(long)} trên thiết bị đã cho (đồng bộ ngoại tuyến, US4). */
    String session(long accountId, String deviceId) {
        String token = SessionTokens.generate();
        LocalDateTime now = utc(time.now());
        jdbc.update("""
                INSERT INTO login_session (token_hash, account_id, device_id, channel, started_at, last_active_at)
                VALUES (?, ?, ?, 'STAFF_APP', ?, ?)
                """, SessionTokens.hash(token), accountId, deviceId, now, now);
        return token;
    }

    /** {@code POST /auth/login} với mã thiết bị đã cho (002 US3). */
    static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(
            String username, String password, String channel, String deviceId) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                .header("X-Device-Id", deviceId)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"channel\":\"" + channel
                        + "\"}");
    }

    static String bearer(String token) {
        return "Bearer " + token;
    }

    private static LocalDateTime utc(java.time.Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
