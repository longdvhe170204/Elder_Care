package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m15.account.internal.auth.service.SessionTokens;
import com.cambae.backend.m15.account.scheduling.MarkInactiveAccountsJob;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 002 T086 (US8 #3; FR-010, FR-013a, SC-013; BR-M15-24): 200 ngày không thao tác thì phiên kênh RESIDENT_APP vẫn còn
 * (CFG-M15-10 không giới hạn) và tài khoản không chuyển Không hoạt động; đổi mật khẩu kết thúc phiên của người cao tuổi.
 * Tasks ghi {@code PhienNguoiCaoTuoiKhongHetIT}.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class ResidentSessionNoTimeoutIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    MarkInactiveAccountsJob job;

    @Autowired
    PasswordEncoder passwords;

    AccountFixtures fixtures;
    ResidentLogins logins;
    long account;

    @BeforeEach
    void setUp() {
        clock.reset();
        fixtures = new AccountFixtures(jdbc, time);
        logins = new ResidentLogins(mvc);
        account = fixtures.residentAccount(AccountFixtures.nextId());
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void SC_013_200DaysIdle_sessionStillOpen_accountStillActive() throws Exception {
        String office = fixtures.session(fixtures.staffAccount("AC-09"));
        String[] session = logins.loggedIn(office, account, UUID.randomUUID().toString());

        clock.advanceBy(Duration.ofDays(200));
        job.runNow();

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(session[0])))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM account WHERE id = ?", String.class, account))
                .isEqualTo("ACTIVE");
    }

    @Test
    void FR_013a_passwordChange_endsResidentSessions() throws Exception {
        String username = fixtures.password(account, "Matkhau123", passwords);
        String body = mvc.perform(AccountFixtures.login(
                        username,
                        "Matkhau123",
                        "RESIDENT_APP",
                        UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JsonPath.read(body, "$.token");

        mvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Matkhau123\",\"newPassword\":\"Matkhau456\"}"))
                .andExpect(status().is2xxSuccessful());

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject(
                        "SELECT end_reason FROM login_session WHERE token_hash = ?",
                        String.class,
                        SessionTokens.hash(token)))
                .isEqualTo("PASSWORD_CHANGED");
    }
}
