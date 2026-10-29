package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.BoundDeviceApi;
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

/**
 * 002 T087 phần không cần 001 (US8 #7, #8; FR-072, FR-073, BR-M15-23; research A-16): Hành chính, Quản lý viện khóa
 * tài khoản người cao tuổi theo yêu cầu của chính người cao tuổi hoặc có lý do (gỡ thiết bị, kết thúc phiên), mở lại khi
 * người cao tuổi yêu cầu; khóa do kết thúc lưu trú không mở được; người thân đề nghị thì Hành chính lập yêu cầu
 * {@code M15.RESIDENT_ACCOUNT_LOCK}, chỉ khóa khi Quản lý viện duyệt. Khóa theo sự kiện kết thúc lưu trú (US8 #6) làm khi
 * 001 khai báo sự kiện.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class ResidentAccountLockIT extends MySqlIntegrationTestBase {

    private static final String CONSENT = "{\"consent\":{\"method\":\"REQUESTED\",\"withStaffId\":7}}";

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
    long account;

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
        logins = new ResidentLogins(mvc);
        office = fixtures.session(fixtures.staffAccount("AC-09"));
        account = fixtures.residentAccount(AccountFixtures.nextId());
    }

    @Test
    void FR_072_lockOnResidentRequest_unbindsDevice_endsSession_thenUnlockWithConsent() throws Exception {
        String[] session = logins.loggedIn(office, account, UUID.randomUUID().toString());

        logins.command(
                        office,
                        account,
                        "lock-resident",
                        version(),
                        "{\"basis\":\"RESIDENT_REQUESTED\",\"reason\":\"Bà muốn tạm ngừng dùng ứng dụng\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andExpect(jsonPath("$.lockCause").value("RESIDENT_REQUESTED"));

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(session[0])))
                .andExpect(status().isUnauthorized());
        assertThat(boundDevices.identify(session[1])).isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT unbind_reason FROM bound_device WHERE account_id = ?", String.class, account))
                .isEqualTo("ACCOUNT_LOCKED");

        logins.command(office, account, "unlock-resident", version(), "{}").andExpect(status().isUnprocessableEntity());
        logins.command(office, account, "unlock-resident", version(), CONSENT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        assertThat(jdbc.queryForObject(
                        "SELECT details FROM audit_log WHERE action = 'ACCOUNT_UNLOCKED' AND entity_id = ?",
                        String.class,
                        String.valueOf(account)))
                .contains("REQUESTED");
    }

    @Test
    void FR_073_lockedByResidencyEnd_cannotBeUnlocked() throws Exception {
        jdbc.update(
                "UPDATE account SET status = 'LOCKED', lock_cause = 'RESIDENT_RESIDENCY_ENDED' WHERE id = ?", account);

        logins.command(office, account, "unlock-resident", version(), CONSENT)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LOCK_CAUSE_STILL_EXISTS"));
    }

    @Test
    void FR_072_onlyOfficeOrManager_notNurse() throws Exception {
        String nurse = fixtures.session(fixtures.staffAccount("AC-04"));

        logins.command(nurse, account, "lock-resident", version(), "{\"basis\":\"WITH_REASON\",\"reason\":\"Thử\"}")
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT status FROM account WHERE id = ?", String.class, account))
                .isEqualTo("ACTIVE");
    }

    @Test
    void A_16_familyRequest_locksOnlyAfterManagerApproves() throws Exception {
        String manager = fixtures.session(fixtures.staffAccount("AC-01"));
        String body = mvc.perform(post("/api/v1/approval-requests")
                        .header("Authorization", bearer(office))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"M15.RESIDENT_ACCOUNT_LOCK\",\"content\":{\"accountId\":" + account
                                + ",\"requestingFamilyMemberId\":55,\"reason\":\"Con gái đề nghị\"},"
                                + "\"requestedEffectiveDate\":\"" + time.today() + "\","
                                + "\"reason\":\"Người thân đề nghị khóa\",\"submitImmediately\":true}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        assertThat(jdbc.queryForObject("SELECT status FROM account WHERE id = ?", String.class, account))
                .isEqualTo("ACTIVE");

        String etag = mvc.perform(get("/api/v1/approval-requests/" + id).header("Authorization", bearer(manager)))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        mvc.perform(post("/api/v1/approval-requests/" + id + "/approve")
                        .header("Authorization", bearer(manager))
                        .header("If-Match", etag)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForMap("SELECT status, lock_cause FROM account WHERE id = ?", account))
                .containsEntry("status", "LOCKED")
                .containsEntry("lock_cause", "FAMILY_REQUEST");
    }

    private String version() {
        return "\"" + jdbc.queryForObject("SELECT version FROM account WHERE id = ?", Long.class, account) + "\"";
    }
}
