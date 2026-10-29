package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static com.eldercare.backend.administration.account.AccountFixtures.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * 002 T040 (FR-014a; BR-M15-15; research A-02): đăng nhập trên thiết bị đã có phiên của tài khoản khác kết thúc phiên cũ
 * trong cùng transaction ({@code DEVICE_SIGNED_IN_ELSEWHERE}); một tài khoản vẫn mở được phiên trên nhiều thiết bị.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class OneDeviceOneSessionIT extends MySqlIntegrationTestBase {

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
    void FR_014a_otherAccountOnSameDevice_endsPreviousSession() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        String sharedDevice = UUID.randomUUID().toString();
        long a = fixtures.staffAccount("AC-05");
        long b = fixtures.staffAccount("AC-05");
        String tokenA = token(fixtures.password(a, PASSWORD, encoder), sharedDevice);
        String tokenB = token(fixtures.password(b, PASSWORD, encoder), sharedDevice);

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(tokenA))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me").header("Authorization", bearer(tokenB))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT end_reason FROM login_session WHERE account_id = ?", String.class, a))
                .isEqualTo("DEVICE_SIGNED_IN_ELSEWHERE");
    }

    @Test
    void FR_014a_sameAccountOnTwoDevices_bothOpen() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long a = fixtures.staffAccount("AC-04");
        String username = fixtures.password(a, PASSWORD, encoder);
        String first = token(username, UUID.randomUUID().toString());
        String second = token(username, UUID.randomUUID().toString());

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(first))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/me").header("Authorization", bearer(second))).andExpect(status().isOk());
    }

    @Test
    void deviceIdMustBeUuid() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long a = fixtures.staffAccount("AC-04");
        String username = fixtures.password(a, PASSWORD, encoder);

        mvc.perform(login(username, PASSWORD, "STAFF_APP", "khong-phai-uuid")).andExpect(status().isBadRequest());
    }

    private String token(String username, String device) throws Exception {
        return json.readTree(mvc.perform(login(username, PASSWORD, "STAFF_APP", device))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("token")
                .asString();
    }
}
