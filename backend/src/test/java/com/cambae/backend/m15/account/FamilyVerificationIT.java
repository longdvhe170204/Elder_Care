package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeFamilyRelationships;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import com.cambae.backend.support.fake.m15.FakeResidencyEnds;
import java.time.Duration;
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

/**
 * 002 T068 (US6 #8, #9; FR-050, FR-051, FR-052; SC-011): Hành chính hoặc Quản lý viện tạo tài khoản người thân cho hồ
 * sơ có quan hệ với người cao tuổi chưa ở trạng thái cuối, chỉ vai trò Người thân, buộc đổi mật khẩu; tạo và cấp lại
 * mật khẩu phải có cách xác minh hợp lệ (gọi lại đúng số đã đăng ký), ghi vào nhật ký, không ghi số.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class FamilyVerificationIT extends MySqlIntegrationTestBase {

    private static final String PHONE = "0901234567";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeFamilyRelationships relationships;

    @Autowired
    FakeResidencyEnds residencyEnds;

    AccountFixtures fixtures;
    String office;
    long familyMember;
    long resident;

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
        office = fixtures.session(fixtures.staffAccount("AC-09"));
        familyMember = AccountFixtures.nextId();
        resident = AccountFixtures.nextId();
        relationships.relate(familyMember, resident);
        relationships.phoneNumber(familyMember, PHONE);
    }

    @Test
    void US6_8_officeCreates_familyRoleOnly_mustChangePassword_verificationAudited() throws Exception {
        create(office, body(familyMember, callback(PHONE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("FAMILY_MEMBER"))
                .andExpect(jsonPath("$.familyMemberId").value(familyMember))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("AC-10"))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty());

        long account =
                jdbc.queryForObject("SELECT id FROM account WHERE family_member_id = ?", Long.class, familyMember);
        assertThat(jdbc.queryForObject("SELECT must_change_password FROM account WHERE id = ?", Boolean.class, account))
                .isTrue();
        String details = jdbc.queryForObject(
                "SELECT details FROM audit_log WHERE action = 'ACCOUNT_CREATED' AND entity_id = ?",
                String.class,
                String.valueOf(account));
        assertThat(details).contains("CALLBACK_REGISTERED_NUMBER").doesNotContain(PHONE);
    }

    @Test
    void US6_8_nurseOrFloorManager_refused() throws Exception {
        for (String role : new String[] {"AC-04", "AC-02"}) {
            String token = fixtures.session(fixtures.staffAccount(role));
            create(token, body(familyMember, callback(PHONE))).andExpect(status().isForbidden());
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM account WHERE family_member_id = ?", Integer.class, familyMember))
                .isZero();
    }

    @Test
    void FR_052_missingOrInvalidVerification_refused() throws Exception {
        create(office, "{\"familyMemberId\":" + familyMember + ",\"username\":\"u" + familyMember + "\"}")
                .andExpect(status().isUnprocessableEntity());
        create(office, body(familyMember, callback("0909999999")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VERIFICATION"));
        create(office, body(familyMember, "{\"method\":\"IN_PERSON_ID_DOCUMENT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VERIFICATION"));
    }

    @Test
    void FR_050_onlyResidentInFinalStatus_refused() throws Exception {
        residencyEnds.ended(resident, time.now().minus(Duration.ofDays(1)));

        create(office, body(familyMember, callback(PHONE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RELATIONSHIP_ENDED"));
    }

    @Test
    void FR_002_profileAlreadyHasAccount_refused() throws Exception {
        fixtures.familyAccount(familyMember);

        create(office, body(familyMember, callback(PHONE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PROFILE_ALREADY_HAS_ACCOUNT"));
    }

    @Test
    void FR_052_resetFamilyPassword_needsVerification_audited() throws Exception {
        long account = fixtures.familyAccount(familyMember);

        resetPassword(account, "{\"reason\":\"Quên mật khẩu\"}").andExpect(status().isUnprocessableEntity());
        resetPassword(account, "{\"reason\":\"Quên mật khẩu\",\"verification\":" + callback("0909999999") + "}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VERIFICATION"));
        resetPassword(account, "{\"reason\":\"Quên mật khẩu\",\"verification\":" + callback(PHONE) + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty());

        assertThat(jdbc.queryForObject("SELECT must_change_password FROM account WHERE id = ?", Boolean.class, account))
                .isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'PASSWORD_RESET' AND entity_id = ?"
                                + " AND JSON_UNQUOTE(JSON_EXTRACT(details, '$.verificationMethod'))"
                                + " = 'CALLBACK_REGISTERED_NUMBER'",
                        Integer.class,
                        String.valueOf(account)))
                .isEqualTo(1);
    }

    @Test
    void FR_051_officeCannotResetStaffPassword() throws Exception {
        long staff = fixtures.staffAccount("AC-05");

        resetPassword(staff, "{\"reason\":\"Quên mật khẩu\",\"verification\":" + callback(PHONE) + "}")
                .andExpect(status().isNotFound());
    }

    private static String body(long familyMember, String verification) {
        return "{\"familyMemberId\":" + familyMember + ",\"username\":\"family-new-" + familyMember
                + "\",\"verification\":" + verification + "}";
    }

    private static String callback(String number) {
        return "{\"method\":\"CALLBACK_REGISTERED_NUMBER\",\"calledPhoneNumber\":\"" + number + "\"}";
    }

    private ResultActions create(String token, String body) throws Exception {
        return mvc.perform(post("/api/v1/family-accounts")
                .header("Authorization", bearer(token))
                .header("X-Requested-From", "web")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions resetPassword(long account, String body) throws Exception {
        return mvc.perform(post("/api/v1/accounts/" + account + "/reset-password")
                .header("Authorization", bearer(office))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
