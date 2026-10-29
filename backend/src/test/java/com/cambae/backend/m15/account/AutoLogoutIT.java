package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.bearer;
import static com.cambae.backend.m15.account.AccountFixtures.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeM15Config;
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

    // Bổ sung 2026-10-29 (api-002 UserInfo.sessionIdleTimeoutSeconds): web biết CFG-M15-10 của kênh để báo trước khi
    // hết
    @Test
    void me_returnsIdleTimeoutOfSessionChannel() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long account = fixtures.staffAccount("AC-09");
        String username = fixtures.password(account, PASSWORD, encoder);
        Cookie cookie = webLogin(username);

        mvc.perform(get("/api/v1/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionIdleTimeoutSeconds").value(1800));
    }

    // Bổ sung 2026-10-29 (FR-013a "không có thao tác"): yêu cầu tự làm mới không đếm lại giờ nhưng vẫn bị kiểm hết hạn
    @Test
    void backgroundRefresh_doesNotResetIdleTimer() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long account = fixtures.staffAccount("AC-09");
        String username = fixtures.password(account, PASSWORD, encoder);
        Cookie cookie = webLogin(username);

        clock.advanceBy(Duration.ofMinutes(20));
        mvc.perform(get("/api/v1/me").cookie(cookie).header("X-Background-Refresh", "true"))
                .andExpect(status().isOk());
        clock.advanceBy(Duration.ofMinutes(15));

        mvc.perform(get("/api/v1/me").cookie(cookie).header("X-Background-Refresh", "true"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SESSION_EXPIRED"));
    }

    // Luồng SSE không gửi được header: tham số backgroundRefresh cùng nghĩa
    @Test
    void backgroundRefreshParam_doesNotResetIdleTimer() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long account = fixtures.staffAccount("AC-09");
        String username = fixtures.password(account, PASSWORD, encoder);
        Cookie cookie = webLogin(username);

        clock.advanceBy(Duration.ofMinutes(20));
        mvc.perform(get("/api/v1/me").cookie(cookie).param("backgroundRefresh", "true"))
                .andExpect(status().isOk());
        clock.advanceBy(Duration.ofMinutes(15));

        mvc.perform(get("/api/v1/me").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    private Cookie webLogin(String username) throws Exception {
        return mvc.perform(login(username, PASSWORD, "WEB", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("session");
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
