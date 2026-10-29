package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.BoundDeviceApi;
import com.eldercare.backend.administration.account.internal.auth.service.SessionTokens;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 002 T085 (US8 #4, #5; FR-070, FR-014a, BR-M15-24): một tài khoản người cao tuổi một thiết bị; gắn máy mới thì máy cũ
 * bị gỡ, phiên trên máy cũ kết thúc; máy đã gắn không đăng nhập được tài khoản khác (409 {@code DEVICE_ALREADY_BOUND});
 * Hành chính, Quản lý viện gỡ thiết bị kèm lý do; mật khẩu dự phòng kênh RESIDENT_APP cũng gắn thiết bị. Tasks ghi
 * {@code ThietBiGanIT}.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class BoundDeviceIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    BoundDeviceApi boundDevices;

    @Autowired
    PasswordEncoder passwords;

    AccountFixtures fixtures;
    ResidentLogins logins;
    String office;
    long account;

    @BeforeEach
    void residentAccount() {
        fixtures = new AccountFixtures(jdbc, time);
        logins = new ResidentLogins(mvc);
        office = fixtures.session(fixtures.staffAccount("AC-09"));
        account = fixtures.residentAccount(AccountFixtures.nextId());
    }

    @Test
    void BR_M15_24_newDevice_unbindsOldDevice_andEndsItsSession() throws Exception {
        String oldDevice = UUID.randomUUID().toString();
        String[] old = logins.loggedIn(office, account, oldDevice);

        String[] fresh = logins.loggedIn(office, account, UUID.randomUUID().toString());

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(old[0]))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me").header("Authorization", bearer(fresh[0]))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT end_reason FROM login_session WHERE token_hash = ?",
                        String.class,
                        SessionTokens.hash(old[0])))
                .isEqualTo("DEVICE_UNBOUND");
        assertThat(jdbc.queryForObject(
                        "SELECT unbind_reason FROM bound_device WHERE device_id = ?", String.class, oldDevice))
                .isEqualTo("NEW_DEVICE_BOUND");
        assertThat(boundDevices.identify(old[1])).isEmpty();
        assertThat(boundDevices.identify(fresh[1])).isPresent();
    }

    @Test
    void FR_014a_boundDevice_cannotSignInAnotherAccount_409() throws Exception {
        String device = UUID.randomUUID().toString();
        logins.loggedIn(office, account, device);
        long other = fixtures.residentAccount(AccountFixtures.nextId());
        long staff = fixtures.staffAccount("AC-05");
        String username = fixtures.password(staff, "Matkhau123", passwords);

        logins.login(logins.code(office, other), device)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DEVICE_ALREADY_BOUND"));
        mvc.perform(AccountFixtures.login(username, "Matkhau123", "STAFF_APP", device))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DEVICE_ALREADY_BOUND"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bound_device WHERE account_id = ?", Integer.class, other))
                .isZero();
    }

    @Test
    void FR_070_unbindWithReason_endsSession_thenNoBoundDevice() throws Exception {
        String device = UUID.randomUUID().toString();
        String[] session = logins.loggedIn(office, account, device);
        String version =
                "\"" + jdbc.queryForObject("SELECT version FROM account WHERE id = ?", Long.class, account) + "\"";

        logins.command(office, account, "unbind-device", version, "{}").andExpect(status().isUnprocessableEntity());
        logins.command(office, account, "unbind-device", "\"999\"", "{\"reason\":\"Báo mất máy\"}")
                .andExpect(status().isPreconditionFailed());
        logins.command(office, account, "unbind-device", version, "{\"reason\":\"Báo mất máy\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boundDevice").doesNotExist());

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(session[0])))
                .andExpect(status().isUnauthorized());
        assertThat(boundDevices.identify(session[1])).isEmpty();
        assertThat(jdbc.queryForMap("SELECT unbind_reason, unbind_note FROM bound_device WHERE device_id = ?", device))
                .containsEntry("unbind_reason", "MANUAL")
                .containsEntry("unbind_note", "Báo mất máy");
        String current =
                "\"" + jdbc.queryForObject("SELECT version FROM account WHERE id = ?", Long.class, account) + "\"";
        logins.command(office, account, "unbind-device", current, "{\"reason\":\"Lần hai\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NO_BOUND_DEVICE"));
    }

    @Test
    void FR_070_detailShowsBoundDevice_unbindOnlyOfficeOrManager() throws Exception {
        String device = UUID.randomUUID().toString();
        logins.loggedIn(office, account, device);
        String nurse = fixtures.session(fixtures.staffAccount("AC-04"));

        mvc.perform(get("/api/v1/accounts/" + account).header("Authorization", bearer(office)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("RESIDENT"))
                .andExpect(jsonPath("$.boundDevice.deviceId").value(device));
        logins.command(nurse, account, "unbind-device", "\"0\"", "{\"reason\":\"Thử\"}")
                .andExpect(status().isForbidden());
        long staff = fixtures.staffAccount("AC-05");
        logins.command(office, staff, "unbind-device", "\"0\"", "{\"reason\":\"Thử\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_RESIDENT_ACCOUNT"));
    }

    @Test
    void A_15_passwordFallback_residentChannel_bindsDevice_returnsDeviceToken() throws Exception {
        String username = fixtures.password(account, "Matkhau123", passwords);
        String device = UUID.randomUUID().toString();

        String body = mvc.perform(AccountFixtures.login(username, "Matkhau123", "RESIDENT_APP", device))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String deviceToken = JsonPath.read(body, "$.deviceToken");
        assertThat(boundDevices.identify(deviceToken))
                .hasValueSatisfying(d -> assertThat(d.deviceId()).isEqualTo(device));
        assertThat(jdbc.queryForObject(
                        "SELECT login_qr_code_id IS NULL FROM bound_device WHERE device_id = ? AND unbound_at IS NULL",
                        Boolean.class,
                        device))
                .isTrue();
    }
}
