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
