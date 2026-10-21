package com.cambae.backend.m15.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.m15.account.api.spi.BedAllocationView;
import com.cambae.backend.m15.account.internal.auth.service.SessionTokens;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeBedAllocations;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

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

    AccountFixtures fixtures;
    ResidentLogins logins;
    String office;
    long residentId;
    long account;

    @BeforeEach
    void residentAccount() {
        clock.reset();
        fixtures = new AccountFixtures(jdbc, time);
        logins = new ResidentLogins(mvc);
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
                        "SELECT device_id FROM bound_device WHERE device_token_hash = ? AND unbound_at IS NULL",
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

    @Test
    void FR_069_onlyForActiveResidentAccounts() throws Exception {
        long staff = fixtures.staffAccount("AC-05");
        jdbc.update("UPDATE account SET status = 'LOCKED', lock_cause = 'WITH_REASON' WHERE id = ?", account);

        logins.createCode(office, staff)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_RESIDENT_ACCOUNT"));
        logins.createCode(office, account)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));
    }
}
