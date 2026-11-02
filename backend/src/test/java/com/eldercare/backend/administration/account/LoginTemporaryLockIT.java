package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static com.eldercare.backend.administration.account.AccountFixtures.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * 002 T036 (US3 #1 → #3; FR-007, FR-008, FR-015; BR-M15-05; research A-03): sai đủ CFG-M15-01 (mặc định 5 lần, 15 phút)
 * thì Khóa tạm, kết thúc phiên; trong thời gian khóa đúng mật khẩu vẫn bị từ chối kèm thời điểm mở; hết thời gian thì
 * trở về Hoạt động ở yêu cầu kế tiếp (nhật ký người thực hiện Bộ lập lịch, căn cứ CFG-M15-01); đăng nhập đúng đưa bộ
 * đếm về 0; mọi lần đăng nhập ghi nhật ký.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class LoginTemporaryLockIT extends MySqlIntegrationTestBase {

    private static final String PASSWORD = "dung12345";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JsonMapper json;

    AccountFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void US3_1_correctPassword_loggedIn_counterReset_lastLoginSaved_audited() throws Exception {
        long account = fixtures.staffAccount("AC-04");
        String username = fixtures.password(account, PASSWORD, encoder);
        jdbc.update("UPDATE account SET failed_login_count = 3 WHERE id = ?", account);

        String body = mvc.perform(login(username, PASSWORD, "STAFF_APP", device()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.user.accountId").value(account))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = json.readTree(body).get("token").asString();

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT failed_login_count FROM account WHERE id = ?", Integer.class, account))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT last_login_at IS NOT NULL FROM account WHERE id = ?", Boolean.class, account))
                .isTrue();
        assertThat(count(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'LOGIN' AND entity_id = ?"
                                + " AND actor_type = 'USER' AND actor_id = ?",
                        String.valueOf(account),
                        account))
                .isEqualTo(1);
    }

    @Test
    void US3_2_fiveWrongPasswords_temporarilyLocked_correctPasswordRefusedWithUnlockTime() throws Exception {
        long account = fixtures.staffAccount("AC-05");
        String username = fixtures.password(account, PASSWORD, encoder);
        String token = fixtures.session(account);

        for (int i = 0; i < 4; i++) {
            mvc.perform(login(username, "sai" + i + "abc", "WEB", device()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("LOGIN_FAILED"));
        }
        assertThat(accountStatus(account)).isEqualTo("ACTIVE");
        mvc.perform(login(username, "sai5abc", "WEB", device()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("LOGIN_FAILED"));

        assertThat(accountStatus(account)).isEqualTo("TEMPORARILY_LOCKED");
        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
        mvc.perform(login(username, PASSWORD, "WEB", device()))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_TEMPORARILY_LOCKED"))
                .andExpect(jsonPath("$.unlocksAt").isNotEmpty());
        assertThat(count(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'ACCOUNT_TEMPORARILY_LOCKED'"
                                + " AND entity_id = ? AND actor_type = 'SCHEDULER'",
                        String.valueOf(account)))
                .isEqualTo(1);
        assertThat(count(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'LOGIN_FAILED' AND entity_id = ?"
                                + " AND actor_type = 'ANONYMOUS'",
                        String.valueOf(account)))
                .isEqualTo(6);
    }

    @Test
    void US3_3_lockExpired_backToActiveAtNextLogin_counterZero_auditedAsScheduler() throws Exception {
        long account = fixtures.staffAccount("AC-05");
        String username = fixtures.password(account, PASSWORD, encoder);
        for (int i = 0; i < 5; i++) {
            mvc.perform(login(username, "sai" + i + "abc", "WEB", device())).andExpect(status().isUnauthorized());
        }
        assertThat(accountStatus(account)).isEqualTo("TEMPORARILY_LOCKED");

        clock.advanceBy(Duration.ofMinutes(15));
        mvc.perform(login(username, PASSWORD, "WEB", device())).andExpect(status().isOk());

        assertThat(accountStatus(account)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT failed_login_count FROM account WHERE id = ?", Integer.class, account))
                .isZero();
        assertThat(count(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'TEMPORARY_LOCK_EXPIRED' AND entity_id = ?"
                                + " AND actor_type = 'SCHEDULER' AND JSON_EXTRACT(basis, '$.ruleCode') = 'CFG-M15-01'",
                        String.valueOf(account)))
                .isEqualTo(1);
        assertThat(count(
                        "SELECT COUNT(*) FROM status_history WHERE entity_type = 'M15.ACCOUNT' AND attribute = 'STATUS' AND entity_id = ?",
                        account))
                .isEqualTo(2);
    }

    @Test
    void FR_015_unknownUsername_sameErrorAsWrongPassword_auditedAnonymousWithUsername() throws Exception {
        String unknown = "khong-co-" + UUID.randomUUID();

        mvc.perform(login(unknown, PASSWORD, "WEB", device()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("LOGIN_FAILED"));

        assertThat(count(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'LOGIN_FAILED' AND actor_type = 'ANONYMOUS'"
                                + " AND entity_id = '' AND JSON_UNQUOTE(JSON_EXTRACT(details, '$.username')) = ?",
                        unknown))
                .isEqualTo(1);
    }

    @Test
    void US3_8_lockedOrInactive_correctPassword_contactFacility() throws Exception {
        long locked = fixtures.staffAccount("AC-05");
        String lockedName = fixtures.password(locked, PASSWORD, encoder);
        jdbc.update("UPDATE account SET status = 'LOCKED', lock_cause = 'MANUAL' WHERE id = ?", locked);
        long inactive = fixtures.familyAccount(AccountFixtures.nextId());
        String inactiveName = fixtures.password(inactive, PASSWORD, encoder);
        jdbc.update("UPDATE account SET status = 'INACTIVE' WHERE id = ?", inactive);

        mvc.perform(login(lockedName, PASSWORD, "WEB", device()))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_CONTACT_FACILITY"));
        mvc.perform(login(inactiveName, PASSWORD, "FAMILY_APP", device()))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_CONTACT_FACILITY"));
        mvc.perform(login(lockedName, "sai12345", "WEB", device()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("LOGIN_FAILED"));
    }

    @Test
    void web_setsHttpOnlyCookie_noTokenInBody() throws Exception {
        long account = fixtures.staffAccount("AC-09");
        String username = fixtures.password(account, PASSWORD, encoder);

        mvc.perform(login(username, PASSWORD, "WEB", device()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                        .httpOnly("session", true));
    }

    private String accountStatus(long account) {
        return jdbc.queryForObject("SELECT status FROM account WHERE id = ?", String.class, account);
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private static String device() {
        return UUID.randomUUID().toString();
    }
}
