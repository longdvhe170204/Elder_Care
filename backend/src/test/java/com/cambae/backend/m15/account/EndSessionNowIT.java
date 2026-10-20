package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.bearer;
import static com.cambae.backend.m15.account.AccountFixtures.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * 002 T040 (FR-013; research A-02): khóa tài khoản thì yêu cầu kế tiếp của phiên đang mở bị 401; đăng xuất kết thúc phiên
 * hiện tại ({@code LOGOUT}).
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class EndSessionNowIT extends MySqlIntegrationTestBase {

    private static final String PASSWORD = "dung12345";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JsonMapper json;

    @Test
    void FR_013_lockedByManager_nextRequest401() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long account = fixtures.staffAccount("AC-05");
        String token = token(fixtures.password(account, PASSWORD, encoder));
        String manager = fixtures.session(fixtures.staffAccount("AC-01"));
        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token))).andExpect(status().isOk());

        mvc.perform(post("/api/v1/accounts/" + account + "/lock")
                        .header("Authorization", bearer(manager))
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Nghỉ dài hạn\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
    }

    @Test
    void FR_013_logout_endsCurrentSession() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long account = fixtures.staffAccount("AC-04");
        String token = token(fixtures.password(account, PASSWORD, encoder));

        mvc.perform(post("/api/v1/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject(
                        "SELECT end_reason FROM login_session WHERE account_id = ?", String.class, account))
                .isEqualTo("LOGOUT");
    }

    private String token(String username) throws Exception {
        return json.readTree(mvc.perform(login(
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
    }
}
