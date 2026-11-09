package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.events.PasswordSelfReset;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationships;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationshipsConfig;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
import com.eldercare.backend.support.fake.notification.FakeM13Config;
import com.eldercare.backend.support.fake.notification.FakeSmsProvider;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 002 T105 (US3 #12 → #14, SC-014; FR-052a, BR-M15-25; CFG-M15-12 → 14 mặc định 5 phút, 5 lần sai, 5 lần mỗi ngày cách
 * 60 giây): mã một lần qua tin nhắn tới số trong hồ sơ; xin mã luôn trả 202 giống nhau; đặt lại xong kết thúc mọi phiên,
 * bộ đếm sai về 0, Khóa tạm trở về Hoạt động, không gắn cờ buộc đổi, báo chủ tài khoản và các Quản lý viện khác. Hai ca
 * (ngân sách IT): luồng chính và các nhánh từ chối.
 */
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import({FakeM15Config.class, FakeFamilyRelationshipsConfig.class, FakeM13Config.class})
class FR_052a_SelfPasswordResetIT extends MySqlIntegrationTestBase {

    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");
    private static final String NEW_PASSWORD = "moi12345";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    FakeSmsProvider sms;

    @Autowired
    FakeStaffProfiles staffProfiles;

    @Autowired
    FakeFamilyRelationships families;

    @Autowired
    ApplicationEvents events;

    AccountFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
        sms.reset();
    }

    @AfterEach
    void resetClock() {
        clock.reset();
        sms.reset();
    }

    @Test
    void US3_12_14_codeToProfilePhone_resetsPassword_endsSessions_unlocks_notifiesOwnerAndManagers() throws Exception {
        mvc.perform(get("/api/v1/auth/password-reset/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        // #12 người thân: mã tới số trong hồ sơ, đổi mật khẩu, mọi phiên kết thúc, nhật ký ghi lại
        long familyMemberId = AccountFixtures.nextId();
        long family = fixtures.familyAccount(familyMemberId);
        String familyName = fixtures.password(family, "cu123456", encoder);
        families.phoneNumber(familyMemberId, "0912000111");
        fixtures.session(family, UUID.randomUUID().toString());

        request(familyName).andExpect(status().isAccepted());
        assertThat(sms.sent())
                .singleElement()
                .satisfies(m -> assertThat(m.phoneNumber()).isEqualTo("0912000111"));
        confirm(familyName, code(0), NEW_PASSWORD).andExpect(status().isNoContent());

        assertThat(encoder.matches(NEW_PASSWORD, hash(family))).isTrue();
        assertThat(jdbc.queryForObject("SELECT must_change_password FROM account WHERE id = ?", Boolean.class, family))
                .isFalse();
        assertThat(openSessions(family)).isZero();
        assertThat(audits("PASSWORD_SELF_RESET", family)).isOne();
        // mã đã dùng không dùng lại được
        confirm(familyName, code(0), "khac12345").andExpect(status().isUnprocessableContent());

        // #14 Quản lý viện đang Khóa tạm: trở về Hoạt động, bộ đếm về 0, các Quản lý viện khác được báo
        long manager = fixtures.staffAccount("AC-01");
        String managerName = fixtures.password(manager, "cu123456", encoder);
        long managerStaff = fixtures.staffId(manager);
        staffProfiles.phone(managerStaff, "0912000222");
        jdbc.update(
                "UPDATE account SET status = 'TEMPORARILY_LOCKED', failed_login_count = 5, locked_until = ?"
                        + " WHERE id = ?",
                java.time.LocalDateTime.ofInstant(time.now().plus(Duration.ofMinutes(10)), java.time.ZoneOffset.UTC),
                manager);

        request(managerName).andExpect(status().isAccepted());
        confirm(managerName, code(1), NEW_PASSWORD).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT status FROM account WHERE id = ?", String.class, manager))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT failed_login_count FROM account WHERE id = ?", Integer.class, manager))
                .isZero();
        List<PasswordSelfReset> resets = events.stream(PasswordSelfReset.class).toList();
        assertThat(resets).hasSize(2);
        assertThat(resets.get(0).notificationRequests())
                .singleElement()
                .satisfies(r -> assertThat(r.recipientGroups())
                        .containsExactly(new RecipientGroup.FamilyMember(familyMemberId, "ACCOUNT_OWNER")));
        assertThat(resets.get(1).notificationRequests()).hasSize(2).anySatisfy(r -> {
            assertThat(r.recipientGroups()).singleElement().isInstanceOf(RecipientGroup.FacilityRole.class);
            assertThat(r.causedBy()).contains(manager);
            assertThat(r.notifyCauser()).isFalse();
        });
    }

    @Test
    void US3_13_14_SC_014_sameResponse_noCodeForIneligible_wrongCodeLimit_expiry_requestLimit() throws Exception {
        // giữa ngày, để các bước tiến đồng hồ không qua nửa đêm (giới hạn theo ngày)
        clock.setTo(time.startOfDay(time.today()).plus(Duration.ofHours(9)));
        long staff = fixtures.staffAccount("AC-04");
        String staffName = fixtures.password(staff, "cu123456", encoder);
        long staffId = fixtures.staffId(staff);
        staffProfiles.phone(staffId, "0912000333");

        // #13, SC-014: tên không tồn tại, không có số, Đã khóa, Không hoạt động, người cao tuổi: cùng 202, không gửi mã
        long noPhone = fixtures.staffAccount("AC-04");
        long locked = fixtures.staffAccount("AC-04");
        jdbc.update("UPDATE account SET status = 'LOCKED', lock_cause = 'MANUAL' WHERE id = ?", locked);
        staffProfiles.phone(fixtures.staffId(locked), "0912000444");
        long inactiveMember = AccountFixtures.nextId();
        long inactive = fixtures.familyAccount(inactiveMember);
        jdbc.update("UPDATE account SET status = 'INACTIVE' WHERE id = ?", inactive);
        families.phoneNumber(inactiveMember, "0912000555");
        long resident = fixtures.residentAccount(AccountFixtures.nextId());
        String body = null;
        for (String username : List.of(
                "khong-ton-tai-" + UUID.randomUUID(),
                username(noPhone),
                username(locked),
                username(inactive),
                username(resident))) {
            String response = request(username)
                    .andExpect(status().isAccepted())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            assertThat(body == null || body.equals(response)).as(username).isTrue();
            body = response;
        }
        assertThat(sms.sent()).isEmpty();

        // sai quá CFG-M15-13 lần thì mã bị hủy, mật khẩu giữ nguyên
        request(staffName).andExpect(status().isAccepted());
        String code = code(0);
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            confirm(staffName, wrong, NEW_PASSWORD)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errorCode").value("RESET_CODE_INVALID"));
        }
        confirm(staffName, code, NEW_PASSWORD).andExpect(jsonPath("$.errorCode").value("RESET_CODE_INVALID"));
        assertThat(encoder.matches("cu123456", hash(staff))).isTrue();

        // xin lại trong CFG-M15-14 (60 giây) không gửi; sau 60 giây gửi; mã hết hạn sau CFG-M15-12 (5 phút)
        request(staffName).andExpect(status().isAccepted());
        assertThat(sms.sent()).hasSize(1);
        clock.advanceBy(Duration.ofSeconds(61));
        request(staffName).andExpect(status().isAccepted());
        assertThat(sms.sent()).hasSize(2);
        clock.advanceBy(Duration.ofMinutes(6));
        confirm(staffName, code(1), NEW_PASSWORD)
                .andExpect(jsonPath("$.errorCode").value("RESET_CODE_INVALID"));

        // mật khẩu mới chưa đạt CFG-M15-09: mã vẫn dùng được sau khi sửa
        request(staffName).andExpect(status().isAccepted());
        confirm(staffName, code(2), "ngan")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("PASSWORD_POLICY_NOT_MET"));
        confirm(staffName, code(2), NEW_PASSWORD).andExpect(status().isNoContent());

        // quá 5 lần mỗi ngày: đã gửi 3 lần, lần thứ 4, 5 được, lần thứ 6 bị chặn
        for (int i = 0; i < 3; i++) {
            clock.advanceBy(Duration.ofSeconds(61));
            request(staffName).andExpect(status().isAccepted());
        }
        assertThat(sms.sent()).hasSize(5);
    }

    private ResultActions request(String username) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\"}"));
    }

    private ResultActions confirm(String username, String code, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"code\":\"" + code + "\",\"newPassword\":\"" + password
                        + "\"}"));
    }

    /** Mã trong tin thứ {@code index} đã gửi. */
    private String code(int index) {
        Matcher m = CODE.matcher(sms.sent().get(index).content());
        assertThat(m.find()).as("tin có mã 6 chữ số").isTrue();
        return m.group(1);
    }

    private String hash(long account) {
        return jdbc.queryForObject("SELECT password_hash FROM account WHERE id = ?", String.class, account);
    }

    private String username(long account) {
        return jdbc.queryForObject("SELECT username FROM account WHERE id = ?", String.class, account);
    }

    private int openSessions(long account) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM login_session WHERE account_id = ? AND ended_at IS NULL", Integer.class, account);
    }

    private int audits(String action, long account) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = ? AND entity_id = ?", Integer.class, action, account);
    }
}
