package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.administration.account.internal.auth.service.SessionTokens;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeBedAllocations;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeResidentProfiles;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    @Test
    void FR_069_expiresAfterCfgM15_11() throws Exception {
        String code = logins.code(office, account);

        clock.advanceBy(Duration.ofMinutes(5).plusSeconds(1));

        logins.login(code, UUID.randomUUID().toString())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_QR_CODE"));
    }

    @Test
    void FR_069_newCodeRevokesOldUnusedCode() throws Exception {
        String first = logins.code(office, account);
        String second = logins.code(office, account);

        logins.login(first, UUID.randomUUID().toString()).andExpect(status().isUnauthorized());
        logins.login(second, UUID.randomUUID().toString()).andExpect(status().isOk());
    }

    @Test
    void FR_069_nurseOnlyWithinScope_othersRefused() throws Exception {
        long floor = AccountFixtures.nextId();
        bedAllocations.allocate(
                residentId,
                new BedAllocationView(
                        AccountFixtures.nextId(), AccountFixtures.nextId(), AccountFixtures.nextId(), floor));
        long nurseInScope = fixtures.staffAccount("AC-04");
        assignments.assign(
                fixtures.staffId(nurseInScope),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        floor,
                        null,
                        null,
                        AccountFixtures.nextId(),
                        time.now().minus(Duration.ofHours(1)),
                        time.now().plus(Duration.ofHours(7))));
        String outOfScope = fixtures.session(fixtures.staffAccount("AC-04"));
        String careStaff = fixtures.session(fixtures.staffAccount("AC-05"));

        logins.createCode(fixtures.session(nurseInScope), account)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").exists());
        logins.createCode(outOfScope, account).andExpect(status().isForbidden());
        logins.createCode(careStaff, account).andExpect(status().isForbidden());
    }

    private static final String CONSENT = ResidentLogins.CONSENT;

    private ResultActions codeForResident(String token, long resident, String body) throws Exception {
        return mvc.perform(post("/api/v1/resident-accounts/" + resident + "/login-qr-code")
                .header("Authorization", bearer(token))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void FR_069_byResident_requiresConsent_recordsIt() throws Exception {
        codeForResident(office, residentId, "{}").andExpect(status().isUnprocessableEntity());

        codeForResident(office, residentId, CONSENT)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").exists());

        assertThat(jdbc.queryForObject(
                        "SELECT details FROM audit_log WHERE action = 'LOGIN_QR_CREATED' AND entity_id = ?"
                                + " ORDER BY id DESC LIMIT 1",
                        String.class,
                        String.valueOf(account)))
                .contains("REQUESTED");
    }

    @Test
    void FR_069_byResident_noAccount_404() throws Exception {
        codeForResident(office, AccountFixtures.nextId(), CONSENT).andExpect(status().isNotFound());
    }

    @Test
    void FR_069_stateShowsBoundDevice_forWarning_withinPermission() throws Exception {
        String url = "/api/v1/resident-accounts/" + residentId;

        mvc.perform(get(url).header("Authorization", bearer(office)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.boundDevice").doesNotExist());
        logins.loggedIn(office, account, UUID.randomUUID().toString());
        mvc.perform(get(url).header("Authorization", bearer(office)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boundDevice.boundAt").exists());
        mvc.perform(get(url).header("Authorization", bearer(fixtures.session(fixtures.staffAccount("AC-05")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_006_qrLogin_neverForcesPasswordChange_voidsPendingTemporaryPassword() throws Exception {
        jdbc.update("UPDATE account SET must_change_password = TRUE, password_hash = 'tam' WHERE id = ?", account);

        String[] session = logins.loggedIn(office, account, UUID.randomUUID().toString());

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(session[0])))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT must_change_password FROM account WHERE id = ?", Boolean.class, account))
                .isFalse();
        assertThat(jdbc.queryForObject("SELECT password_hash FROM account WHERE id = ?", String.class, account))
                .isNotEqualTo("tam");
    }

    @Test
    void BR_M15_23_finalStatus_refusesCreatingAndUsingCode() throws Exception {
        String code = logins.code(office, account);
        residents.resident(residentId, "Nguyễn Thị Hoa", false, true);

        logins.login(code, UUID.randomUUID().toString())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_QR_CODE"));
        logins.createCode(office, account)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESIDENCY_ENDED"));
    }

    @Test
    void FR_069_onlyForActiveResidentAccounts() throws Exception {
        jdbc.update("UPDATE account SET status = 'LOCKED', lock_cause = 'WITH_REASON' WHERE id = ?", account);

        logins.createCode(office, account)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));
    }

    @Test
    void FR_069_codeByAccountId_removed_onlyByResident() throws Exception {
        mvc.perform(post("/api/v1/accounts/" + account + "/login-qr-code")
                        .header("Authorization", bearer(office))
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(r -> assertThat(r.getResponse().getStatus()).isIn(404, 405));
    }

    @Test
    void FR_069_FR_051a_allowedCommands_lockedAccount_noCodeNoReset() throws Exception {
        mvc.perform(get("/api/v1/accounts/" + account).header("Authorization", bearer(office)))
                .andExpect(jsonPath("$.allowedCommands").value(hasItems("login-qr-code", "reset-password")));

        jdbc.update("UPDATE account SET status = 'LOCKED', lock_cause = 'WITH_REASON' WHERE id = ?", account);

        mvc.perform(get("/api/v1/accounts/" + account).header("Authorization", bearer(office)))
                .andExpect(jsonPath("$.allowedCommands").value(hasItem("unlock-resident")))
                .andExpect(jsonPath("$.allowedCommands").value(not(hasItem("login-qr-code"))))
                .andExpect(jsonPath("$.allowedCommands").value(not(hasItem("reset-password"))));
    }
}
