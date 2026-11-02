package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.AccessChannel;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.internal.auth.service.SessionAuthentication;
import com.eldercare.backend.administration.account.internal.auth.service.SessionService;
import com.eldercare.backend.administration.account.internal.auth.service.SessionTokens;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 002 T012 (FR-044; 000 FR-045; contracts/module-interfaces.md mục 1.1): {@code CurrentUser} trong yêu cầu là người
 * dùng của phiên (loại, liên kết hồ sơ, vai trò đang hiệu lực, thiết bị, kênh); khi job chạy ({@code @Scheduled},
 * {@code ScheduledJob.runNow}) là Bộ lập lịch, kể cả job chạy trong một yêu cầu có phiên; không có cả hai thì lỗi.
 */
@Import(CurrentUserIT.Jobs.class)
class CurrentUserIT extends MySqlIntegrationTestBase {

    private static final AtomicLong STAFF_IDS = new AtomicLong(910_000);

    @Autowired
    CurrentUser currentUser;

    @Autowired
    SessionService sessions;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    Jobs.ProbeJob job;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void FR_044_inRequest_isSessionUserWithActiveRoles() {
        long staffId = STAFF_IDS.incrementAndGet();
        long accountId = account(staffId);
        assignRole(accountId, "AC-02", time.now().minus(Duration.ofDays(1)), null);
        assignRole(accountId, "AC-04", time.now().minus(Duration.ofDays(1)), null);
        assignRole(
                accountId,
                "AC-01",
                time.now().minus(Duration.ofDays(2)),
                time.now().minus(Duration.ofDays(1)));
        String deviceId = UUID.randomUUID().toString();
        signIn(accountId, deviceId, "STAFF_APP");

        assertThat(currentUser.accountId()).isEqualTo(accountId);
        assertThat(currentUser.type()).isEqualTo(AccountType.STAFF);
        assertThat(currentUser.staffId()).contains(staffId);
        assertThat(currentUser.familyMemberId()).isEmpty();
        assertThat(currentUser.residentId()).isEmpty();
        assertThat(currentUser.roles()).containsExactlyInAnyOrder(Role.FLOOR_MANAGER, Role.NURSING_STAFF);
        assertThat(currentUser.isScheduler()).isFalse();
        assertThat(currentUser.deviceId()).isEqualTo(deviceId);
        assertThat(currentUser.channel()).isEqualTo(AccessChannel.STAFF_APP);
    }

    @Test
    void FR_045_whileScheduledMethodRuns_isScheduler() {
        job.scheduled();

        Seen seen = job.seen();
        assertThat(seen.scheduler()).isTrue();
        assertThat(seen.roles()).containsExactly(Role.SCHEDULER);
    }

    @Test
    void FR_045_jobRunNowInsideSessionRequest_isScheduler_thenSessionUserAgain() {
        long accountId = account(STAFF_IDS.incrementAndGet());
        signIn(accountId, UUID.randomUUID().toString(), "WEB");

        job.runNow();

        assertThat(job.seen().scheduler()).isTrue();
        assertThat(currentUser.isScheduler()).isFalse();
        assertThat(currentUser.accountId()).isEqualTo(accountId);
    }

    @Test
    void noSessionAndNoJob_hasNoCurrentUser() {
        assertThatThrownBy(() -> currentUser.accountId()).isInstanceOf(IllegalStateException.class);
    }

    private long account(long staffId) {
        LocalDateTime now = utc(time.now());
        jdbc.update("""
                INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password,
                                     inactivity_reference_at, created_at)
                VALUES (?, 'STAFF', ?, 'ACTIVE', 'x', FALSE, ?, ?)
                """, "current-user-it-" + staffId, staffId, now, now);
        return jdbc.queryForObject("SELECT id FROM account WHERE staff_id = ?", Long.class, staffId);
    }

    private void assignRole(long accountId, String role, Instant from, Instant to) {
        jdbc.update(
                "INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, effective_to, reason)"
                        + " VALUES ('ROLE_ASSIGNMENT', 'ACCOUNT', ?, ?, ?, ?, ?)",
                accountId,
                role,
                utc(from),
                to == null ? null : utc(to),
                "Test");
    }

    /** Như {@code SessionTokenFilter}: tra phiên rồi đặt vào {@code SecurityContext}. */
    private void signIn(long accountId, String deviceId, String channel) {
        String token = SessionTokens.generate();
        LocalDateTime now = utc(time.now());
        jdbc.update("""
                INSERT INTO login_session (token_hash, account_id, device_id, channel, started_at, last_active_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, SessionTokens.hash(token), accountId, deviceId, channel, now, now);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new SessionAuthentication(sessions.resolve(token).orElseThrow()));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    record Seen(boolean scheduler, Set<Role> roles) {}

    @TestConfiguration
    static class Jobs {

        @Bean
        ProbeJob probeJob(CurrentUser currentUser) {
            return new ProbeJob(currentUser);
        }

        /** Job thử: ghi lại người dùng hiện tại lúc chạy. Cron {@code -} nên không tự chạy. */
        static class ProbeJob implements ScheduledJob {

            private final AtomicReference<Seen> seen = new AtomicReference<>();
            private final CurrentUser currentUser;

            ProbeJob(CurrentUser currentUser) {
                this.currentUser = currentUser;
            }

            /** Gọi qua proxy của Spring (aspect Bộ lập lịch), nên đọc bằng phương thức, không đọc trường. */
            public Seen seen() {
                return seen.get();
            }

            @Override
            public String name() {
                return "PROBE";
            }

            @Override
            public void runNow() {
                seen.set(snapshot());
            }

            @Scheduled(cron = "-")
            public void scheduled() {
                seen.set(snapshot());
            }

            private Seen snapshot() {
                return new Seen(currentUser.isScheduler(), currentUser.roles());
            }
        }
    }
}
