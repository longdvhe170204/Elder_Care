package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.bearer;
import static com.cambae.backend.m15.account.AccountFixtures.login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * 002 T079a (quickstart 002 mục 1): dữ liệu mẫu {@code demo/V002_90} có một tài khoản cho mỗi vai trò nhân viên AC-01
 * → 05, 07, 09, 14 và Quản lý viện thứ hai; đăng nhập được bằng mật khẩu demo, không buộc đổi mật khẩu, {@code /me} trả
 * đúng vai trò.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class SampleAccountsIT extends MySqlIntegrationTestBase {

    private static final String DEMO_PASSWORD = "Demo2026!";

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper json;

    @ParameterizedTest
    @CsvSource({
        "demo.quanly, AC-01",
        "demo.quanly2, AC-01",
        "demo.truongtang, AC-02",
        "demo.bacsi, AC-03",
        "demo.dieuduong, AC-04",
        "demo.chamsoc, AC-05",
        "demo.bep, AC-07",
        "demo.hanhchinh, AC-09",
        "demo.ketoan, AC-14"
    })
    void T079a_sampleAccount_logsInWithDemoPassword_hasRole(String username, String role) throws Exception {
        String token = json.readTree(mvc.perform(login(
                                username,
                                DEMO_PASSWORD,
                                "STAFF_APP",
                                UUID.randomUUID().toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.mustChangePassword").value(false))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("token")
                .asString();

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("STAFF"))
                .andExpect(jsonPath("$.roles[0]").value(role))
                .andExpect(jsonPath("$.roles.length()").value(1));
    }
}
