package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.spi.StaffStatus;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
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
 * 002 T029 (US2 #5, #6; FR-006, FR-011, FR-013; Q-292): Quản lý viện khóa tài khoản có lý do và {@code If-Match}, phiên
 * kết thúc; người đang là Phụ trách bộ phận phải có người thay (gọi 008 cùng transaction); mở khóa bị từ chối khi nguyên
 * nhân tự động còn; cấp lại mật khẩu buộc đổi ở lần đăng nhập sau; chi tiết tài khoản có ETag và {@code allowedCommands}.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class LockUnlockIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeStaffProfiles staffProfiles;

    @Autowired
    FakeDuties duties;

    AccountFixtures fixtures;
    String manager;

    @BeforeEach
    void managerSignedIn() {
        fixtures = new AccountFixtures(jdbc, time);
        manager = fixtures.session(fixtures.staffAccount("AC-01"));
    }

    @Test
    void US2_5_lockWithoutReason_422_withReason_lockedAndSessionsEnded() throws Exception {
        long account = fixtures.staffAccount("AC-05");
        String token = fixtures.session(account);

        command(account, "lock", "\"0\"", "{}").andExpect(status().isUnprocessableEntity());
        command(account, "lock", "\"0\"", "{\"reason\":\"Vi phạm nội quy\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andExpect(jsonPath("$.lockCause").value("MANUAL"))
                .andExpect(header().string("ETag", "\"1\""));

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
    }

    @Test
    void FR_040_staleIfMatch_412() throws Exception {
        long account = fixtures.staffAccount("AC-05");

        command(account, "lock", "\"7\"", "{\"reason\":\"Vi phạm nội quy\"}")
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    void US2_6_unlockWhileStillResigned_409_afterBackToWork_unlocked() throws Exception {
        long account = fixtures.staffAccount("AC-05");
        long staffId = fixtures.staffId(account);
        jdbc.update("UPDATE account SET status = 'LOCKED', lock_cause = 'RESIGNATION' WHERE id = ?", account);
        staffProfiles.status(staffId, StaffStatus.RESIGNED);

        command(account, "unlock", "\"0\"", "{\"reason\":\"Đi làm lại\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LOCK_CAUSE_STILL_EXISTS"));

        staffProfiles.status(staffId, StaffStatus.WORKING);
        command(account, "unlock", "\"0\"", "{\"reason\":\"Đi làm lại\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void Q_292_departmentLead_needsReplacement_replacedInSameCommand() throws Exception {
        long account = fixtures.staffAccount("AC-07");
        long lead = fixtures.staffId(account);
        long replacement = AccountFixtures.nextId();
        long kitchen = AccountFixtures.nextId();
        duties.department(lead, kitchen).departmentLead(kitchen, lead);

        command(account, "lock", "\"0\"", "{\"reason\":\"Chuyển công tác\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REPLACEMENT_LEAD_REQUIRED"));
        command(
                        account,
                        "lock",
                        "\"0\"",
                        "{\"reason\":\"Chuyển công tác\",\"replacementDepartmentLead\":" + replacement + "}")
                .andExpect(status().isOk());

        assertThat(duties.departmentLead(kitchen)).contains(replacement);
    }

    @Test
    void FR_006_resetPassword_temporaryPasswordOnce_mustChange_audited() throws Exception {
        long account = fixtures.staffAccount("AC-05");
        String before = jdbc.queryForObject("SELECT password_hash FROM account WHERE id = ?", String.class, account);

        mvc.perform(post("/api/v1/accounts/" + account + "/reset-password")
                        .header("Authorization", bearer(manager))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Quên mật khẩu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty());

        assertThat(jdbc.queryForMap("SELECT password_hash, must_change_password FROM account WHERE id = ?", account))
                .containsEntry("must_change_password", true)
                .doesNotContainEntry("password_hash", before);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'PASSWORD_RESET' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(account)))
                .isEqualTo(1);
    }

    @Test
    void FR_042_detail_hasEtag_andAllowedCommandsByStatus() throws Exception {
        long active = fixtures.staffAccount("AC-05");
        long locked = fixtures.staffAccount("AC-05");
        jdbc.update("UPDATE account SET status = 'LOCKED', lock_cause = 'MANUAL', version = 3 WHERE id = ?", locked);

        mvc.perform(get("/api/v1/accounts/" + active).header("Authorization", bearer(manager)))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.roles[0]").value("AC-05"))
                .andExpect(jsonPath(
                        "$.allowedCommands",
                        containsInAnyOrder(
                                "lock",
                                "reset-password",
                                "assign-role",
                                "remove-role",
                                "restrict",
                                "restore-permission")));
        mvc.perform(get("/api/v1/accounts/" + locked).header("Authorization", bearer(manager)))
                .andExpect(header().string("ETag", "\"3\""))
                .andExpect(jsonPath(
                        "$.allowedCommands",
                        containsInAnyOrder(
                                "unlock",
                                "reset-password",
                                "assign-role",
                                "remove-role",
                                "restrict",
                                "restore-permission")));
    }

    @Test
    void FR_051_list_managerSeesStaff_officeSeesOnlyFamilyAccounts() throws Exception {
        long account = fixtures.staffAccount("AC-05");
        String office = fixtures.session(fixtures.staffAccount("AC-09"));
        String username = jdbc.queryForObject("SELECT username FROM account WHERE id = ?", String.class, account);

        mvc.perform(get("/api/v1/accounts").param("q", username).header("Authorization", bearer(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(account));
        mvc.perform(get("/api/v1/accounts").param("q", username).header("Authorization", bearer(office)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    private ResultActions command(long account, String command, String ifMatch, String body) throws Exception {
        return mvc.perform(post("/api/v1/accounts/" + account + "/" + command)
                .header("Authorization", bearer(manager))
                .header("If-Match", ifMatch)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
