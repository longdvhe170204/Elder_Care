package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 002 T046, T047 (đổi api-002 2026-10-28, chủ dự án duyệt; FR-013a, CFG-M15-10): app chung đăng nhập một màn với kênh
 * {@code MOBILE_APP}; backend gán kênh phiên theo loại tài khoản (nhân viên STAFF_APP, người thân FAMILY_APP) để thời
 * gian tự đăng xuất theo kênh giữ nguyên; tài khoản người cao tuổi không đăng nhập bằng mật khẩu ở kênh này (FR-069).
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class MobileAppLoginIT extends MySqlIntegrationTestBase {

    private static final String PASSWORD = "dung12345";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    PasswordEncoder encoder;

    @Test
    void FR_013a_staffOnMobileApp_sessionChannelStaffApp() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long staff = fixtures.staffAccount("AC-04");
        String username = fixtures.password(staff, PASSWORD, encoder);

        mvc.perform(login(username, PASSWORD, "MOBILE_APP", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.type").value("STAFF"));

        assertThat(channelOf(staff)).isEqualTo("STAFF_APP");
    }

    @Test
    void FR_013a_familyOnMobileApp_sessionChannelFamilyApp() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long family = fixtures.familyAccount(AccountFixtures.nextId());
        String username = fixtures.password(family, PASSWORD, encoder);

        mvc.perform(login(username, PASSWORD, "MOBILE_APP", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.type").value("FAMILY_MEMBER"));

        assertThat(channelOf(family)).isEqualTo("FAMILY_APP");
    }

    @Test
    void FR_069_residentOnMobileApp_refused_noSession() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long resident = fixtures.residentAccount(AccountFixtures.nextId());
        String username = fixtures.password(resident, PASSWORD, encoder);

        mvc.perform(login(username, PASSWORD, "MOBILE_APP", UUID.randomUUID().toString()))
                .andExpect(status().isUnprocessableContent());

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM login_session WHERE account_id = ?", Integer.class, resident))
                .isZero();
    }

    private String channelOf(long accountId) {
        return jdbc.queryForObject(
                "SELECT channel FROM login_session WHERE account_id = ? AND ended_at IS NULL", String.class, accountId);
    }
}
