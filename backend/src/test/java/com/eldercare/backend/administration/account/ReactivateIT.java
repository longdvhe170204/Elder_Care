package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationships;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeResidencyEnds;
import java.time.Duration;
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
 * 002 T042 (bảng trạng thái "Kích hoạt lại"; FR-011, FR-051, FR-052; SC-011): Hành chính hoặc Quản lý viện kích hoạt lại
 * tài khoản người thân Không hoạt động kèm lý do và cách xác minh; gọi lại phải đúng số đã đăng ký; người thân phải còn
 * quan hệ Hiệu lực với người cao tuổi chưa ở trạng thái cuối hoặc chưa quá CFG-M01-04 (mặc định 30 ngày); tài khoản về
 * Hoạt động, buộc đổi mật khẩu, mốc tính Không hoạt động đặt lại; nhật ký ghi cách xác minh.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class ReactivateIT extends MySqlIntegrationTestBase {

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

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
        office = fixtures.session(fixtures.staffAccount("AC-09"));
    }

    @Test
    void US6_9_callbackRegisteredNumber_reactivated_mustChangePassword_audited() throws Exception {
        long familyMember = AccountFixtures.nextId();
        long account = inactive(familyMember);
        relationships.relate(familyMember, AccountFixtures.nextId());
        relationships.phoneNumber(familyMember, PHONE);

        reactivate(account, callback(PHONE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(jdbc.queryForObject("SELECT must_change_password FROM account WHERE id = ?", Boolean.class, account))
                .isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'ACCOUNT_REACTIVATED' AND entity_id = ?"
                                + " AND reason IS NOT NULL"
                                + " AND JSON_UNQUOTE(JSON_EXTRACT(details, '$.verificationMethod'))"
                                + " = 'CALLBACK_REGISTERED_NUMBER'",
                        Integer.class,
                        String.valueOf(account)))
                .isEqualTo(1);
    }

    @Test
    void FR_052_numberJustReadByRequester_refused() throws Exception {
        long familyMember = AccountFixtures.nextId();
        long account = inactive(familyMember);
        relationships.relate(familyMember, AccountFixtures.nextId());
        relationships.phoneNumber(familyMember, PHONE);

        reactivate(account, callback("0909999999"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VERIFICATION"));
        reactivate(account, "{\"reason\":\"Thiếu xác minh\"}").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void FR_011_residentEndedOver30Days_refused_within30Days_allowed() throws Exception {
        long familyMember = AccountFixtures.nextId();
        long account = inactive(familyMember);
        long resident = AccountFixtures.nextId();
        relationships.relate(familyMember, resident);
        relationships.phoneNumber(familyMember, PHONE);
        residencyEnds.ended(resident, time.now().minus(Duration.ofDays(31)));

        reactivate(account, callback(PHONE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RELATIONSHIP_ENDED"));

        long other = AccountFixtures.nextId();
        relationships.relate(familyMember, other);
        residencyEnds.ended(other, time.now().minus(Duration.ofDays(10)));
        reactivate(account, callback(PHONE)).andExpect(status().isOk());
    }

    @Test
    void onlyInactiveFamilyAccounts() throws Exception {
        long staff = fixtures.staffAccount("AC-05");
        long familyMember = AccountFixtures.nextId();
        long active = fixtures.familyAccount(familyMember);
        relationships.relate(familyMember, AccountFixtures.nextId());
        relationships.phoneNumber(familyMember, PHONE);

        reactivate(active, callback(PHONE)).andExpect(status().isConflict());
        reactivate(staff, callback(PHONE)).andExpect(status().isNotFound());
    }

    private long inactive(long familyMember) {
        long account = fixtures.familyAccount(familyMember);
        jdbc.update("UPDATE account SET status = 'INACTIVE' WHERE id = ?", account);
        return account;
    }

    private static String callback(String number) {
        return "{\"reason\":\"Người thân gọi xin mở lại\",\"verification\":{\"method\":\"CALLBACK_REGISTERED_NUMBER\","
                + "\"calledPhoneNumber\":\"" + number + "\"}}";
    }

    private ResultActions reactivate(long account, String body) throws Exception {
        String version = jdbc.queryForObject("SELECT version FROM account WHERE id = ?", String.class, account);
        return mvc.perform(post("/api/v1/accounts/" + account + "/reactivate")
                .header("Authorization", bearer(office))
                .header("If-Match", "\"" + version + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
