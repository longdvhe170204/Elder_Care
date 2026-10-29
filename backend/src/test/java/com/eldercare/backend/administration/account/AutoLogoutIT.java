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
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * 002 T039 (US3 #11; FR-013a; BR-M15-21): phiên đăng nhập thật tự kết thúc khi không thao tác quá CFG-M15-10 của kênh
 * (web 30 phút, app nhân viên 12 giờ), tài khoản vẫn Hoạt động.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class AutoLogoutIT extends MySqlIntegrationTestBase {

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

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void US3_11_web40MinutesIdle_sessionEnded_accountStillActive() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long account = fixtures.staffAccount("AC-09");
        String username = fixtures.password(account, PASSWORD, encoder);
        Cookie cookie = mvc.perform(
                        login(username, PASSWORD, "WEB", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("session");

        clock.advanceBy(Duration.ofMinutes(40));

        mvc.perform(get("/api/v1/me").cookie(cookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SESSION_EXPIRED"));
        assertThat(jdbc.queryForObject("SELECT status FROM account WHERE id = ?", String.class, account))
                .isEqualTo("ACTIVE");
    }

    @Test
    void US3_11_staffApp150MinutesIdle_stillUsable() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long account = fixtures.staffAccount("AC-04");
        String username = fixtures.password(account, PASSWORD, encoder);
        String token = json.readTree(mvc.perform(login(
                                username,
                                PASSWORD,
                                "STAFF_APP",
                                UUID.randomUUID().toString()))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("token")
                .asString();

        clock.advanceBy(Duration.ofMinutes(150));

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token))).andExpect(status().isOk());
    }
}
