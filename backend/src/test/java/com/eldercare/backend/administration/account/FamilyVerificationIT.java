package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.RelationshipFixtures;
import com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeResidencyEnds;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
import org.springframework.transaction.support.TransactionTemplate;

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
    RelationshipHistoryStore history;

    @Autowired
    TransactionTemplate tx;

    RelationshipFixtures family;

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
        family = new RelationshipFixtures(
                jdbc, tx, history, time.now().minus(Duration.ofDays(10)).truncatedTo(ChronoUnit.MICROS));
        familyMember = family.familyMember("Người thân thử", PHONE);
        resident = AccountFixtures.nextId();
        family.relate(familyMember, resident, 1, false, false);
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
