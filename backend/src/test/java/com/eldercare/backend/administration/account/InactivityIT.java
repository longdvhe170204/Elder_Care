package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static com.eldercare.backend.administration.account.AccountFixtures.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.scheduling.MarkInactiveAccountsJob;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 002 T036, T043 (US3 #6, #7; FR-010, FR-013; BR-M15-05; NFR-04, NFR-05): job chỉ chuyển tài khoản người thân Hoạt động
 * có mốc tính quá CFG-M15-02 (mặc định 180 ngày) sang Không hoạt động, kết thúc phiên, nhật ký Bộ lập lịch kèm căn cứ;
 * nhân viên không bị chuyển; chạy lại không chuyển hai lần; đăng nhập sau đó được hướng dẫn liên hệ viện.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class InactivityIT extends MySqlIntegrationTestBase {

    private static final String PASSWORD = "dung12345";

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
    MarkInactiveAccountsJob job;

    AccountFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void US3_6_familyOver180Days_inactive_sessionsEnded_loginContactFacility() throws Exception {
        long family = fixtures.familyAccount(AccountFixtures.nextId());
        String username = fixtures.password(family, PASSWORD, encoder);
        reference(family, Duration.ofDays(181));
        String token = fixtures.session(family);
        long recent = fixtures.familyAccount(AccountFixtures.nextId());
        reference(recent, Duration.ofDays(179));

        job.runNow();

        assertThat(accountStatus(family)).isEqualTo("INACTIVE");
        assertThat(accountStatus(recent)).isEqualTo("ACTIVE");
        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'ACCOUNT_INACTIVE' AND entity_id = ?"
                                + " AND actor_type = 'SCHEDULER' AND JSON_EXTRACT(basis, '$.ruleCode') = 'CFG-M15-02'",
                        Integer.class,
                        String.valueOf(family)))
                .isEqualTo(1);
        mvc.perform(login(username, PASSWORD, "FAMILY_APP", UUID.randomUUID().toString()))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_CONTACT_FACILITY"));
    }

    @Test
    void US3_7_staffNeverInactive() {
        long staff = fixtures.staffAccount("AC-05");
        reference(staff, Duration.ofDays(400));

        job.runNow();

        assertThat(accountStatus(staff)).isEqualTo("ACTIVE");
    }

    @Test
    void NFR_05_rerun_noSecondTransition() {
        long family = fixtures.familyAccount(AccountFixtures.nextId());
        reference(family, Duration.ofDays(200));

        job.runNow();
        job.runNow();

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM account_status_history WHERE account_id = ? AND status = 'INACTIVE'",
                        Integer.class,
                        family))
                .isEqualTo(1);
    }

    @Test
    void FR_010_loginResetsReference() throws Exception {
        long family = fixtures.familyAccount(AccountFixtures.nextId());
        String username = fixtures.password(family, PASSWORD, encoder);
        reference(family, Duration.ofDays(179));
        mvc.perform(login(username, PASSWORD, "FAMILY_APP", UUID.randomUUID().toString()))
                .andExpect(status().isOk());

        clock.advanceBy(Duration.ofDays(5));
        job.runNow();

        assertThat(accountStatus(family)).isEqualTo("ACTIVE");
    }

    /** Đặt mốc tính Không hoạt động lùi về trước thời điểm hiện tại. */
    private void reference(long account, Duration ago) {
        jdbc.update(
                "UPDATE account SET inactivity_reference_at = ? WHERE id = ?",
                LocalDateTime.ofInstant(time.now().minus(ago), ZoneOffset.UTC),
                account);
    }

    private String accountStatus(long account) {
        return jdbc.queryForObject("SELECT status FROM account WHERE id = ?", String.class, account);
    }
}
