package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static com.eldercare.backend.administration.account.AccountFixtures.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.events.AccountCreated;
import com.eldercare.backend.administration.account.api.events.AccountStatusChanged;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
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
 * 002 FR-015 (module-interfaces mục 1.10): tạo tài khoản phát {@code AccountCreated}; mỗi lần chuyển trạng thái phát
 * {@code AccountStatusChanged} với trạng thái trước, sau và nguyên nhân khóa (rỗng khi trạng thái mới không phải Đã
 * khóa).
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
@RecordApplicationEvents
class FR_015_AccountEventsIT extends MySqlIntegrationTestBase {

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
    FakeStaffProfiles staffProfiles;

    @Autowired
    ApplicationEvents events;

    AccountFixtures fixtures;
    String manager;

    @BeforeEach
    void managerSignedIn() {
        fixtures = new AccountFixtures(jdbc, time);
        manager = fixtures.session(fixtures.staffAccount("AC-01"));
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void FR_015_createStaffAccount_publishesAccountCreated() throws Exception {
        long staffId = AccountFixtures.nextId();
        staffProfiles.staff(staffId, Role.NURSING_STAFF);

        mvc.perform(post("/api/v1/accounts")
                        .header("Authorization", bearer(manager))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staffId\":" + staffId + ",\"username\":\"su-kien-" + staffId
                                + "\",\"roles\":[\"AC-04\"]}"))
                .andExpect(status().isCreated());

        long accountId = jdbc.queryForObject("SELECT id FROM account WHERE staff_id = ?", Long.class, staffId);
        assertThat(events.stream(AccountCreated.class).filter(e -> e.accountId() == accountId))
                .containsExactly(new AccountCreated(accountId, AccountType.STAFF));
    }

    @Test
    void FR_015_lockThenUnlock_publishesEachTransitionWithCause() throws Exception {
        long account = fixtures.staffAccount("AC-05");

        command(account, "lock", "\"0\"").andExpect(status().isOk());
        command(account, "unlock", "\"1\"").andExpect(status().isOk());

        assertThat(statusEvents(account))
                .containsExactly(
                        new AccountStatusChanged(account, AccountStatus.ACTIVE, AccountStatus.LOCKED, "MANUAL"),
                        new AccountStatusChanged(account, AccountStatus.LOCKED, AccountStatus.ACTIVE, null));
    }

    @Test
    void FR_015_temporaryLockAndExpiry_publishedWithoutCause() throws Exception {
        long account = fixtures.staffAccount("AC-05");
        String username = fixtures.password(account, PASSWORD, encoder);
        for (int i = 0; i < 5; i++) {
            mvc.perform(login(
                            username,
                            "sai" + i + "abc",
                            "WEB",
                            UUID.randomUUID().toString()))
                    .andExpect(status().isUnauthorized());
        }
        clock.advanceBy(Duration.ofMinutes(15));
        mvc.perform(login(username, PASSWORD, "WEB", UUID.randomUUID().toString()))
                .andExpect(status().isOk());

        assertThat(statusEvents(account))
                .containsExactly(
                        new AccountStatusChanged(account, AccountStatus.ACTIVE, AccountStatus.TEMPORARILY_LOCKED, null),
                        new AccountStatusChanged(
                                account, AccountStatus.TEMPORARILY_LOCKED, AccountStatus.ACTIVE, null));
    }

    private List<AccountStatusChanged> statusEvents(long account) {
        return events.stream(AccountStatusChanged.class)
                .filter(e -> e.accountId() == account)
                .toList();
    }

    private ResultActions command(long account, String name, String ifMatch) throws Exception {
        return mvc.perform(post("/api/v1/accounts/" + account + "/" + name)
                .header("Authorization", bearer(manager))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("If-Match", ifMatch)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Kiểm sự kiện\"}"));
    }
}
