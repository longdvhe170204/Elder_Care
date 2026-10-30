package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import com.cambae.backend.support.fake.m15.FakeStaffProfiles;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
 * 002 T037 (US3 #4, #5; FR-019c; BR-M15-05; SC-011): mở khóa sớm tài khoản Khóa tạm: Quản lý viện; Trưởng tầng được giao
 * tầng, hoặc Người phụ trách ca của tầng, với nhân viên có phân công cùng tầng trong ca đang diễn ra; lý do và xác minh
 * trực tiếp bắt buộc; bộ đếm sai về 0; nhật ký ghi căn cứ. Người phụ trách ca tầng khác, tài khoản người thân, Hành
 * chính bị từ chối.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class EarlyUnlockIT extends MySqlIntegrationTestBase {

    private static final long FLOOR_1 = 9_101;
    private static final long FLOOR_2 = 9_102;
    private static final long NIGHT_SHIFT = 7_001;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeStaffProfiles staffProfiles;

    AccountFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
    }

    @Test
    void US3_4_manager_earlyUnlocks_counterZero_audited() throws Exception {
        long locked = temporarilyLocked(fixtures.staffAccount("AC-05"));
        String manager = fixtures.session(fixtures.staffAccount("AC-01"));

        earlyUnlock(manager, locked, "{\"reason\":\"Đã gặp trực tiếp\",\"verifiedInPerson\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(jdbc.queryForObject("SELECT failed_login_count FROM account WHERE id = ?", Integer.class, locked))
                .isZero();
        assertThat(audit(locked, "NURSING_HOME_MANAGER")).isEqualTo(1);
    }

    @Test
    void US3_5_shiftLeadSameFloorAndShift_allowed_dutyBasisAudited() throws Exception {
        long nurse = temporarilyLocked(fixtures.staffAccount("AC-04"));
        assignments.assign(fixtures.staffId(nurse), shift(AssignmentKind.SHIFT_MEMBER, FLOOR_2));
        long lead = fixtures.staffAccount("AC-04");
        assignments.assign(fixtures.staffId(lead), shift(AssignmentKind.SHIFT_LEAD, FLOOR_2));

        earlyUnlock(fixtures.session(lead), nurse, "{\"reason\":\"Gặp D ở quầy trực\",\"verifiedInPerson\":true}")
                .andExpect(status().isOk());

        assertThat(audit(nurse, "SHIFT_LEAD")).isEqualTo(1);
    }

    @Test
    void FR_019c_floorManagerOfFloor_allowed() throws Exception {
        long carer = temporarilyLocked(fixtures.staffAccount("AC-05"));
        assignments.assign(fixtures.staffId(carer), shift(AssignmentKind.SHIFT_MEMBER, FLOOR_2));
        long floorManager = fixtures.staffAccount("AC-02");
        assignments.assign(fixtures.staffId(floorManager), shift(AssignmentKind.FLOOR_MANAGER, FLOOR_2));

        earlyUnlock(fixtures.session(floorManager), carer, "{\"reason\":\"Đã gặp\",\"verifiedInPerson\":true}")
                .andExpect(status().isOk());

        assertThat(audit(carer, "FLOOR_MANAGER")).isEqualTo(1);
    }

    @Test
    void US3_5_shiftLeadOtherFloor_familyAccount_officeStaff_refused() throws Exception {
        long nurse = temporarilyLocked(fixtures.staffAccount("AC-04"));
        assignments.assign(fixtures.staffId(nurse), shift(AssignmentKind.SHIFT_MEMBER, FLOOR_2));
        long otherLead = fixtures.staffAccount("AC-04");
        assignments.assign(fixtures.staffId(otherLead), shift(AssignmentKind.SHIFT_LEAD, FLOOR_1));
        long family = temporarilyLocked(fixtures.familyAccount(AccountFixtures.nextId()));
        long lead = fixtures.staffAccount("AC-04");
        assignments.assign(fixtures.staffId(lead), shift(AssignmentKind.SHIFT_LEAD, FLOOR_2));
        String office = fixtures.session(fixtures.staffAccount("AC-09"));
        String body = "{\"reason\":\"Thử\",\"verifiedInPerson\":true}";

        earlyUnlock(fixtures.session(otherLead), nurse, body).andExpect(status().isForbidden());
        earlyUnlock(fixtures.session(lead), family, body).andExpect(status().isForbidden());
        earlyUnlock(office, nurse, body).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT status FROM account WHERE id = ?", String.class, nurse))
                .isEqualTo("TEMPORARILY_LOCKED");
    }

    @Test
    void FR_019c_candidates_onlyTemporarilyLockedStaffOfMyFloorAndShift() throws Exception {
        long nurse = temporarilyLocked(fixtures.staffAccount("AC-04"));
        staffProfiles.name(fixtures.staffId(nurse), "[Họ tên NV 1]");
        assignments.assign(fixtures.staffId(nurse), shift(AssignmentKind.SHIFT_MEMBER, FLOOR_2));
        long otherFloor = temporarilyLocked(fixtures.staffAccount("AC-05"));
        assignments.assign(fixtures.staffId(otherFloor), shift(AssignmentKind.SHIFT_MEMBER, FLOOR_1));
        long expired = fixtures.staffAccount("AC-05");
        jdbc.update(
                "UPDATE account SET status = 'TEMPORARILY_LOCKED', locked_until = ? WHERE id = ?",
                LocalDateTime.ofInstant(time.now().minus(Duration.ofMinutes(1)), ZoneOffset.UTC),
                expired);
        assignments.assign(fixtures.staffId(expired), shift(AssignmentKind.SHIFT_MEMBER, FLOOR_2));
        long lead = fixtures.staffAccount("AC-04");
        assignments.assign(fixtures.staffId(lead), shift(AssignmentKind.SHIFT_LEAD, FLOOR_2));
        String office = fixtures.session(fixtures.staffAccount("AC-09"));

        mvc.perform(get("/api/v1/early-unlock-candidates").header("Authorization", bearer(fixtures.session(lead))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].accountId", hasItem((int) nurse)))
                .andExpect(jsonPath("$.items[*].accountId", not(hasItem((int) otherFloor))))
                .andExpect(jsonPath("$.items[*].accountId", not(hasItem((int) expired))))
                .andExpect(jsonPath("$.items[?(@.accountId == " + nurse + ")].fullName")
                        .value("[Họ tên NV 1]"))
                .andExpect(jsonPath("$.items[?(@.accountId == " + nurse + ")].lockedUntil")
                        .isNotEmpty())
                .andExpect(jsonPath("$.items[?(@.accountId == " + nurse + ")].version")
                        .value(0));
        mvc.perform(get("/api/v1/early-unlock-candidates").header("Authorization", bearer(office)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].accountId", not(hasItem((int) nurse))));
    }

    @Test
    void FR_019c_reasonAndInPersonVerificationRequired_onlyTemporarilyLocked() throws Exception {
        long locked = temporarilyLocked(fixtures.staffAccount("AC-05"));
        String manager = fixtures.session(fixtures.staffAccount("AC-01"));
        long manuallyLocked = fixtures.staffAccount("AC-05");
        jdbc.update("UPDATE account SET status = 'LOCKED', lock_cause = 'MANUAL' WHERE id = ?", manuallyLocked);

        earlyUnlock(manager, locked, "{\"verifiedInPerson\":true}").andExpect(status().isUnprocessableEntity());
        earlyUnlock(manager, locked, "{\"reason\":\"Thiếu xác minh\"}").andExpect(status().isUnprocessableEntity());
        earlyUnlock(manager, manuallyLocked, "{\"reason\":\"Sai lệnh\",\"verifiedInPerson\":true}")
                .andExpect(status().isConflict());
    }

    private long temporarilyLocked(long account) {
        jdbc.update(
                "UPDATE account SET status = 'TEMPORARILY_LOCKED', locked_until = ?, failed_login_count = 5 WHERE id = ?",
                LocalDateTime.ofInstant(time.now().plus(Duration.ofMinutes(10)), ZoneOffset.UTC),
                account);
        return account;
    }

    /** Phân công gắn ca đêm đang diễn ra trên tầng đã cho. */
    private AssignmentView shift(AssignmentKind kind, long floor) {
        Instant now = time.now();
        return new AssignmentView(
                kind, floor, null, null, NIGHT_SHIFT, now.minus(Duration.ofHours(2)), now.plus(Duration.ofHours(6)));
    }

    private ResultActions earlyUnlock(String token, long account, String body) throws Exception {
        String version = jdbc.queryForObject("SELECT version FROM account WHERE id = ?", String.class, account);
        return mvc.perform(post("/api/v1/accounts/" + account + "/early-unlock")
                .header("Authorization", bearer(token))
                .header("If-Match", "\"" + version + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private int audit(long account, String authority) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'ACCOUNT_EARLY_UNLOCKED' AND entity_id = ?"
                        + " AND reason IS NOT NULL AND JSON_UNQUOTE(JSON_EXTRACT(details, '$.authority')) = ?"
                        + " AND JSON_EXTRACT(details, '$.verifiedInPerson') = true",
                Integer.class,
                String.valueOf(account),
                authority);
    }
}
