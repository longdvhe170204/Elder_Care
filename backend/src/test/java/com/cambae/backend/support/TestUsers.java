package com.cambae.backend.support;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.AccessChannel;
import com.cambae.backend.m15.account.api.dto.AccountType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Người dùng thật cho test tích hợp của 001, 003–019 (002 T080; tasks ghi {@code QuyenGia}): tài khoản Hoạt động, vai
 * trò ghi thẳng vào bảng của 002, nên quyền tính bằng {@code PermissionApi} thật theo Phụ lục 27. Phạm vi lấy từ bản giả
 * SPI của {@code FakeM15Config} ({@code FakeAssignments}, {@code FakeBedAllocations}, {@code FakeFamilyRelationships}…):
 * test đặt phân công cho {@link TestUser#staffId()}. Test của 000 vẫn dùng {@link FakePermissions}. Không qua lệnh tạo tài
 * khoản (cần quyền Quản lý viện, hồ sơ 008) để test của module khác không phụ thuộc luồng đó.
 */
public class TestUsers {

    // id hồ sơ nhân viên, người thân giả: tách khỏi dải của AccountFixtures (002) và dữ liệu mẫu demo (9001 → 9009)
    private static final AtomicLong IDS = new AtomicLong(5_000_000);
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Tài khoản đã dựng: {@code staffId} hoặc {@code familyMemberId} trong {@code user()} là id để đặt phân công, quan hệ. */
    public record TestUser(long accountId, long staffId, CurrentUser user) {}

    private record User(
            long accountId,
            AccountType type,
            Optional<Long> staffId,
            Optional<Long> familyMemberId,
            Set<Role> roles,
            String deviceId,
            AccessChannel channel)
            implements CurrentUser {

        @Override
        public Optional<Long> residentId() {
            return Optional.empty();
        }

        @Override
        public boolean isScheduler() {
            return false;
        }
    }

    private final JdbcTemplate jdbc;
    private final BusinessTime time;

    public TestUsers(JdbcTemplate jdbc, BusinessTime time) {
        this.jdbc = jdbc;
        this.time = time;
    }

    /** Id chưa dùng cho hồ sơ, tầng, phòng, người cao tuổi giả trong test. */
    public static long nextId() {
        return IDS.incrementAndGet();
    }

    /** Tài khoản nhân viên Hoạt động có các vai trò đã cho, như phiên app nhân viên. */
    public TestUser staff(Role... roles) {
        long staffId = nextId();
        long accountId = insert("STAFF", "staff_id", staffId, "test-staff-" + staffId);
        Set<Role> set = EnumSet.noneOf(Role.class);
        for (Role role : roles) {
            assignRole(accountId, role);
            set.add(role);
        }
        return new TestUser(
                accountId,
                staffId,
                new User(
                        accountId,
                        AccountType.STAFF,
                        Optional.of(staffId),
                        Optional.empty(),
                        Set.copyOf(set),
                        "test-device-" + accountId,
                        AccessChannel.STAFF_APP));
    }

    /** Tài khoản người thân Hoạt động (vai trò AC-10) của hồ sơ người thân đã cho; {@code staffId} là 0. */
    public TestUser family(long familyMemberId) {
        long accountId = insert("FAMILY_MEMBER", "family_member_id", familyMemberId, "test-family-" + familyMemberId);
        assignRole(accountId, Role.FAMILY_MEMBER);
        return new TestUser(
                accountId,
                0,
                new User(
                        accountId,
                        AccountType.FAMILY_MEMBER,
                        Optional.empty(),
                        Optional.of(familyMemberId),
                        Set.of(Role.FAMILY_MEMBER),
                        "test-device-" + accountId,
                        AccessChannel.FAMILY_APP));
    }

    /** Tài khoản người cao tuổi Hoạt động (vai trò AC-16) của hồ sơ đã cho; trả id tài khoản. */
    public long resident(long residentId) {
        long accountId = insert("RESIDENT", "resident_id", residentId, "test-resident-" + residentId);
        assignRole(accountId, Role.RESIDENT);
        return accountId;
    }

    /** Mở phiên kênh app nhân viên cho tài khoản; trả token cho {@link #bearer(String)}. */
    public String session(long accountId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = utc(time.now());
        jdbc.update("""
                INSERT INTO login_session (token_hash, account_id, device_id, channel, started_at, last_active_at)
                VALUES (?, ?, ?, 'STAFF_APP', ?, ?)
                """, sha256(token), accountId, UUID.randomUUID().toString(), now, now);
        return token;
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    private long insert(String type, String idColumn, long id, String username) {
        LocalDateTime created = utc(time.now().minus(Duration.ofDays(1)));
        jdbc.update(
                "INSERT INTO account (username, type, " + idColumn + ", status, password_hash, must_change_password,"
                        + " inactivity_reference_at, created_at) VALUES (?, ?, ?, 'ACTIVE', 'x', FALSE, ?, ?)",
                username,
                type,
                id,
                created,
                created);
        return jdbc.queryForObject("SELECT id FROM account WHERE username = ?", Long.class, username);
    }

    private void assignRole(long accountId, Role role) {
        jdbc.update(
                "INSERT INTO role_assignment (account_id, role, effective_from, reason) VALUES (?, ?, ?, 'Test')",
                accountId,
                role.code(),
                utc(time.now().minus(Duration.ofDays(1))));
    }

    /** Như {@code SessionTokens.hash} của 002 (SHA-256); chép lại để test của module khác không dùng gói internal. */
    private static byte[] sha256(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static LocalDateTime utc(java.time.Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
