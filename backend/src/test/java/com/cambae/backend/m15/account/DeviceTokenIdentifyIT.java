package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.BoundDeviceApi;
import com.cambae.backend.m15.account.api.dto.ResidentDevice;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 002 T088 (FR-071, SC-013; research A-15; module-interfaces 1.11): {@code BoundDeviceApi.identify} xác định người cao
 * tuổi từ token thiết bị mà không cần phiên; rỗng khi token sai, thiết bị đã gỡ, tài khoản Đã khóa; Khóa tạm vẫn xác
 * định được. Ba đường không cần phiên (đăng nhập, đăng nhập bằng mã QR, {@code /sos/**}); mọi đường khác vẫn đòi phiên.
 * Tasks ghi {@code XacDinhThietBiSosIT}, {@code DuongKhongCanPhienIT}.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class DeviceTokenIdentifyIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    BoundDeviceApi boundDevices;

    AccountFixtures fixtures;
    ResidentLogins logins;
    String office;
    long residentId;
    long account;

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
        logins = new ResidentLogins(mvc);
        office = fixtures.session(fixtures.staffAccount("AC-09"));
        residentId = AccountFixtures.nextId();
        account = fixtures.residentAccount(residentId);
    }

    @Test
    void FR_071_identify_withoutSession() throws Exception {
        String device = UUID.randomUUID().toString();
        String[] session = logins.loggedIn(office, account, device);

        assertThat(boundDevices.identify(session[1])).contains(new ResidentDevice(residentId, account, device));
        assertThat(boundDevices.identify("sai-" + session[1])).isEmpty();
        assertThat(boundDevices.identify("")).isEmpty();
    }

    @Test
    void FR_071_temporarilyLocked_stillIdentified_locked_empty() throws Exception {
        String[] session = logins.loggedIn(office, account, UUID.randomUUID().toString());

        jdbc.update(
                "UPDATE account SET status = 'TEMPORARILY_LOCKED', locked_until = ? WHERE id = ?",
                java.time.LocalDateTime.now().plusHours(1),
                account);
        assertThat(boundDevices.identify(session[1])).isPresent();

        jdbc.update(
                "UPDATE account SET status = 'LOCKED', lock_cause = 'WITH_REASON', locked_until = NULL WHERE id = ?",
                account);
        assertThat(boundDevices.identify(session[1])).isEmpty();
    }

    @Test
    void A_15_onlyThreePublicPaths_sosNeedsNoSession() throws Exception {
        mvc.perform(post("/api/v1/sos/press"))
                .andExpect(r -> assertThat(r.getResponse().getStatus()).isNotEqualTo(401));
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/roles")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/accounts/" + account + "/login-qr-code")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me").header("Authorization", bearer("khong-phai-phien")))
                .andExpect(status().isUnauthorized());
    }
}
