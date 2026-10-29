package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.BoundDeviceApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeResidentProfiles;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.resident.FakeResidentEvents;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 002 T087 #6 (US8 #6; FR-072 (a), FR-073, BR-M15-23; research A-16): người cao tuổi sang trạng thái cuối thì tài khoản
 * Đã khóa nguyên nhân {@code RESIDENT_RESIDENCY_ENDED} ngay (không chờ CFG-M01-04), kết thúc phiên, gỡ thiết bị; sự kiện
 * giao lại không thêm lần khóa; không mở lại được, kể cả khi trước đó đã khóa theo yêu cầu. Sự kiện phát như M01
 * ({@link FakeResidentEvents}); listener của M01 gọi {@code AccountApi.lockResidentOnFinalStatus}.
 */
@AutoConfigureMockMvc
@Import({FakeM15Config.class, FakeM01Config.class})
class ResidentAccountFinalStatusIT extends MySqlIntegrationTestBase {

    private static final String CONSENT = "{\"consent\":{\"method\":\"REQUESTED\",\"withStaffId\":7}}";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    FakeResidentEvents residentEvents;

    @Autowired
    FakeResidentProfiles residents;

    @Autowired
    BoundDeviceApi boundDevices;

    AccountFixtures fixtures;
    ResidentLogins logins;
    String office;
    long resident;
    long account;

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
        logins = new ResidentLogins(mvc);
        office = fixtures.session(fixtures.staffAccount("AC-09"));
        resident = AccountFixtures.nextId();
        account = fixtures.residentAccount(resident);
    }

    private void statusChanged(ResidentStatus to, StatusCommand command) {
        tx.executeWithoutResult(
                s -> residentEvents.statusChanged(resident, ResidentStatus.IN_RESIDENCE, to, command, time.now()));
    }

    private String lockCause() {
        return jdbc.queryForObject("SELECT lock_cause FROM account WHERE id = ?", String.class, account);
    }

    private int lockedHistoryRows() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM account_status_history WHERE account_id = ? AND status = 'LOCKED'",
                Integer.class,
                account);
    }

    private String version() {
        return "\"" + jdbc.queryForObject("SELECT version FROM account WHERE id = ?", Long.class, account) + "\"";
    }

    @Test
    void BR_M15_23_deceased_locksAtOnce_endsSession_unbindsDevice_onceOnly() throws Exception {
        String[] session = logins.loggedIn(office, account, UUID.randomUUID().toString());

        statusChanged(ResidentStatus.DECEASED, StatusCommand.RECORD_DEATH);

        await().atMost(Duration.ofSeconds(10)).until(() -> "RESIDENT_RESIDENCY_ENDED".equals(lockCause()));
        assertThat(boundDevices.identify(session[1])).isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM login_session WHERE account_id = ? AND ended_at IS NULL",
                        Integer.class,
                        account))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT basis FROM audit_log WHERE action = 'ACCOUNT_LOCKED' AND entity_id = ?",
                        String.class,
                        String.valueOf(account)))
                .contains("BR-M15-23");

        statusChanged(ResidentStatus.DECEASED, StatusCommand.RECORD_DEATH);
        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(5)).until(() -> true);
        assertThat(lockedHistoryRows()).isEqualTo(1);

        logins.command(office, account, "unlock-resident", version(), CONSENT)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LOCK_CAUSE_STILL_EXISTS"));
    }

    @Test
    void FR_072_otherStatus_noLock() throws Exception {
        statusChanged(ResidentStatus.HOSPITALIZED, StatusCommand.TRANSFER_OUT);
        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(5)).until(() -> true);

        assertThat(jdbc.queryForObject("SELECT status FROM account WHERE id = ?", String.class, account))
                .isEqualTo("ACTIVE");
    }

    @Test
    void FR_073_lockedOnRequest_thenResidencyEnded_cannotBeUnlocked() throws Exception {
        logins.command(
                        office,
                        account,
                        "lock-resident",
                        version(),
                        "{\"basis\":\"RESIDENT_REQUESTED\",\"reason\":\"Ông muốn tạm ngừng dùng ứng dụng\"}")
                .andExpect(status().isOk());

        statusChanged(ResidentStatus.RESIDENCY_ENDED, StatusCommand.END_RESIDENCY);
        await().atMost(Duration.ofSeconds(10)).until(() -> "RESIDENT_RESIDENCY_ENDED".equals(lockCause()));
        residents.resident(resident, "Lê Văn Cường", false, true);

        logins.command(office, account, "unlock-resident", version(), CONSENT)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LOCK_CAUSE_STILL_EXISTS"));
    }
}
