package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static com.eldercare.backend.administration.account.AccountFixtures.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * 002 T039 (US3 #9; FR-006; research A-03): cấp lại mật khẩu thì lần đăng nhập kế tiếp phải đổi mật khẩu; trước khi đổi,
 * phiên chỉ gọi được đổi mật khẩu và đăng xuất ({@code 403 PASSWORD_CHANGE_REQUIRED} với mọi đường khác); đổi xong thì
 * dùng được, cờ được gỡ, nhật ký không chứa mật khẩu.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class MustChangePasswordIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    JsonMapper json;

    @Test
    void US3_9_resetPassword_mustChangeBeforeOtherFunctions() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        String manager = fixtures.session(fixtures.staffAccount("AC-01"));
        long account = fixtures.staffAccount("AC-04");
        String username = jdbc.queryForObject("SELECT username FROM account WHERE id = ?", String.class, account);
        String temporary = json.readTree(mvc.perform(post("/api/v1/accounts/" + account + "/reset-password")
                                .header("Authorization", bearer(manager))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Quên mật khẩu\"}"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("temporaryPassword")
                .asString();

        String token = json.readTree(mvc.perform(login(
                                username,
                                temporary,
                                "STAFF_APP",
                                UUID.randomUUID().toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.mustChangePassword").value(true))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("token")
                .asString();

        mvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PASSWORD_CHANGE_REQUIRED"));
        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
        changePassword(token, temporary, "moi123456").andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT must_change_password FROM account WHERE id = ?", Boolean.class, account))
                .isFalse();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'PASSWORD_CHANGED' AND entity_id = ?"
                                + " AND COALESCE(data_after, '') NOT LIKE '%moi123456%'",
                        Integer.class, String.valueOf(account)))
                .isEqualTo(1);
    }

    @Test
    void FR_006_wrongCurrentPassword_422_unchanged() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long account = fixtures.staffAccount("AC-05");
        String token = fixtures.session(account);

        changePassword(token, "khong-dung1", "moi123456").andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT password_hash FROM account WHERE id = ?", String.class, account))
                .isEqualTo("x");
    }

    org.springframework.test.web.servlet.ResultActions changePassword(String token, String current, String next)
            throws Exception {
        return mvc.perform(post("/api/v1/auth/change-password")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}"));
    }
}
