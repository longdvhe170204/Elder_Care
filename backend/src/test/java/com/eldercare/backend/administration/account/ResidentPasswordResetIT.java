package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** 002 T102 (FR-051a, FR-006; 19.1): Hành chính, Quản lý viện cấp lại mật khẩu dự phòng khi chính người cao tuổi yêu cầu. */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class ResidentPasswordResetIT extends MySqlIntegrationTestBase {

    private static final String WITH_CONSENT =
            "{\"reason\":\"Bà quên mật khẩu\",\"consent\":{\"method\":\"REQUESTED\",\"withStaffId\":7}}";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    AccountFixtures fixtures;
    String office;
    long residentId;
    long account;

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
        office = fixtures.session(fixtures.staffAccount("AC-09"));
        residentId = AccountFixtures.nextId();
        account = fixtures.residentAccount(residentId);
    }

    private ResultActions reset(String token, String body) throws Exception {
        return mvc.perform(post("/api/v1/accounts/" + account + "/reset-password")
                .header("Authorization", bearer(token))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void FR_051a_officeResets_withResidentConsent_passwordLoginMustChange() throws Exception {
        String body = reset(office, WITH_CONSENT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String temporary = JsonPath.read(body, "$.temporaryPassword");

        assertThat(jdbc.queryForObject(
                        "SELECT details FROM audit_log WHERE action = 'PASSWORD_RESET' AND entity_id = ?",
                        String.class,
                        String.valueOf(account)))
                .contains("REQUESTED");
        mvc.perform(AccountFixtures.login(
                        "resident-" + residentId,
                        temporary,
                        "RESIDENT_APP",
                        UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.mustChangePassword").value(true));
    }

    @Test
    void FR_051a_withoutResidentConsent_422_passwordUnchanged() throws Exception {
        reset(office, "{\"reason\":\"Người thân nhờ\"}").andExpect(status().isUnprocessableEntity());

        assertThat(jdbc.queryForObject("SELECT must_change_password FROM account WHERE id = ?", Boolean.class, account))
                .isFalse();
    }

    @Test
    void FR_051a_nurse_cannotReset() throws Exception {
        String nurse = fixtures.session(fixtures.staffAccount("AC-04"));

        reset(nurse, WITH_CONSENT).andExpect(status().isForbidden());
    }
}
