package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 002 T039 (US3 #10; FR-006a; BR-M15-21; research A-03): mật khẩu mới theo CFG-M15-09 (mặc định tối thiểu 8 ký tự, có chữ
 * và số) và khác mật khẩu hiện tại; không đạt thì 422 {@code PASSWORD_POLICY_NOT_MET} kèm yêu cầu cần đạt.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class PasswordStrengthIT extends MySqlIntegrationTestBase {

    private static final String CURRENT = "hientai123";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    PasswordEncoder encoder;

    @Test
    void US3_10_lettersOnly_refusedWithRequirement_lettersAndDigits_accepted() throws Exception {
        String token = session();

        change(token, "abcdefgh")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("PASSWORD_POLICY_NOT_MET"))
                .andExpect(jsonPath("$.unmetConditions[0].code").value("CFG-M15-09"));
        change(token, "abcd1234").andExpect(status().isNoContent());
    }

    @Test
    void FR_006a_tooShort_or_sameAsCurrent_refused() throws Exception {
        String token = session();

        change(token, "ab12").andExpect(status().isUnprocessableEntity());
        change(token, CURRENT)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("PASSWORD_POLICY_NOT_MET"));
    }

    private String session() {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long account = fixtures.staffAccount("AC-05");
        fixtures.password(account, CURRENT, encoder);
        return fixtures.session(account);
    }

    private ResultActions change(String token, String next) throws Exception {
        return mvc.perform(post("/api/v1/auth/change-password")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + CURRENT + "\",\"newPassword\":\"" + next + "\"}"));
    }
}
