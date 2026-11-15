package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.internal.auth.service.SessionTokens;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeBedAllocations;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeResidentProfiles;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 002 T084 (US8 #2; FR-069, FR-070; research A-15): mã QR dùng một lần, hết hạn sau CFG-M15-11 (5 phút), mã mới vô hiệu
 * mã cũ; Hành chính, Quản lý viện, Điều dưỡng có phạm vi tạo được (Phụ lục 27 ⁴⁸), Điều dưỡng ngoài phạm vi 403; dùng mã
 * thì gắn thiết bị, mở phiên kênh RESIDENT_APP, trả token thiết bị. Tasks ghi {@code MaQrDangNhapIT}.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
@Tag("BR-78")
class LoginQrCodeIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeBedAllocations bedAllocations;

    @Autowired
    FakeResidentProfiles residents;

    AccountFixtures fixtures;
    ResidentLogins logins;
    String office;
    long residentId;
    long account;

    @BeforeEach
    void residentAccount() {
        clock.reset();
        fixtures = new AccountFixtures(jdbc, time);
        logins = new ResidentLogins(mvc, jdbc);
        office = fixtures.session(fixtures.staffAccount("AC-09"));
        residentId = AccountFixtures.nextId();
        account = fixtures.residentAccount(residentId);
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void FR_069_codeIsSingleUse_bindsDevice_opensResidentSession() throws Exception {
        String code = logins.code(office, account);
        String device = UUID.randomUUID().toString();

        String body = logins.login(code, device)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.type").value("RESIDENT"))
                .andExpect(jsonPath("$.user.roles[0]").value("AC-16"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JsonPath.read(body, "$.token");
        String deviceToken = JsonPath.read(body, "$.deviceToken");

        assertThat(jdbc.queryForObject(
                        "SELECT channel FROM login_session WHERE token_hash = ?",
                        String.class,
                        SessionTokens.hash(token)))
                .isEqualTo("RESIDENT_APP");
        assertThat(jdbc.queryForObject(
                        "SELECT device_id FROM device_credential WHERE kind = 'BOUND_DEVICE' AND secret_hash = ?"
                                + " AND ended_at IS NULL",
                        String.class,
                        SessionTokens.hash(deviceToken)))
                .isEqualTo(device);
        assertThat(jdbc.queryForList(
                        "SELECT action FROM audit_log WHERE entity_type = 'M15.ACCOUNT' AND entity_id = ?",
                        String.class,
                        String.valueOf(account)))
                .contains("LOGIN_QR_CREATED", "LOGIN_QR_USED", "DEVICE_BOUND");

        logins.login(code, UUID.randomUUID().toString())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_QR_CODE"));
    }

    private static final String CONSENT = ResidentLogins.CONSENT;

    private ResultActions codeForResident(String token, long resident, String body) throws Exception {
        return mvc.perform(post("/api/v1/resident-accounts/" + resident + "/login-qr-code")
                .header("Authorization", bearer(token))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
