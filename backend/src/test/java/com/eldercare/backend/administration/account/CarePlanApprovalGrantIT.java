package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 002 T074 (US7 #2, Independent Test; FR-024, Q-07): Quản lý viện gán quyền Duyệt kế hoạch chăm sóc cho một tài khoản
 * Điều dưỡng kèm lý do; điều dưỡng đó có quyền theo phân công, điều dưỡng khác không; chỉ gán cho tài khoản đang có vai
 * trò Điều dưỡng; gỡ thì mất; quyền chỉ tính khi tài khoản còn vai trò Điều dưỡng; nhật ký ghi trước, sau.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class CarePlanApprovalGrantIT extends MySqlIntegrationTestBase {

    private static final String CARE_PLAN_APPROVE =
            "$[?(@.function == 'CARE_PLAN' && @.operation == 'APPROVE' && @.source == 'ADDITIONAL_GRANT')]";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    AccountFixtures fixtures;
    PermissionCommands commands;
    String manager;

    @BeforeEach
    void managerSignedIn() {
        fixtures = new AccountFixtures(jdbc, time);
        commands = new PermissionCommands(mvc, jdbc);
        manager = fixtures.session(fixtures.staffAccount("AC-01"));
    }

    @Test
    void US7_2_grantToOneNurse_otherNurseNot_thenRevoked_audited() throws Exception {
        long nurse = fixtures.staffAccount("AC-04");
        String nurseToken = fixtures.session(nurse);
        String otherNurse = fixtures.session(fixtures.staffAccount("AC-04"));

        commands.account(manager, nurse, "grant-care-plan-approval", "\"0\"", PermissionCommands.REASON)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.additionalGrants[0].function").value("CARE_PLAN"))
                .andExpect(jsonPath("$.additionalGrants[0].scope").value("ASSIGNED"));
        mvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(nurseToken)))
                .andExpect(jsonPath(CARE_PLAN_APPROVE).isNotEmpty())
                .andExpect(jsonPath(CARE_PLAN_APPROVE + ".scope").value("ASSIGNED"));
        mvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(otherNurse)))
                .andExpect(jsonPath(CARE_PLAN_APPROVE).isEmpty());

        commands.account(manager, nurse, "grant-care-plan-approval", "\"1\"", PermissionCommands.REASON)
                .andExpect(status().isConflict());
        commands.account(manager, nurse, "revoke-care-plan-approval", "\"1\"", PermissionCommands.REASON)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.additionalGrants").isEmpty());
        mvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(nurseToken)))
                .andExpect(jsonPath(CARE_PLAN_APPROVE).isEmpty());
        commands.account(manager, nurse, "revoke-care-plan-approval", "\"2\"", PermissionCommands.REASON)
                .andExpect(status().isConflict());

        assertThat(jdbc.queryForList(
                        "SELECT action FROM audit_log WHERE entity_type = 'M15.ACCOUNT' AND entity_id = ? ORDER BY id",
                        String.class,
                        String.valueOf(nurse)))
                .containsExactly("ADDITIONAL_PERMISSION_GRANTED", "ADDITIONAL_PERMISSION_REVOKED");
    }

    @Test
    void FR_042_allowedCommands_grantOrRevoke_onlyForNurse() throws Exception {
        long nurse = fixtures.staffAccount("AC-04");
        long careStaff = fixtures.staffAccount("AC-05");

        mvc.perform(get("/api/v1/accounts/" + nurse).header("Authorization", bearer(manager)))
                .andExpect(jsonPath("$.allowedCommands", hasItem("grant-care-plan-approval")))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("revoke-care-plan-approval"))));
        commands.account(manager, nurse, "grant-care-plan-approval", "\"0\"", PermissionCommands.REASON)
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/accounts/" + nurse).header("Authorization", bearer(manager)))
                .andExpect(jsonPath("$.allowedCommands", hasItem("revoke-care-plan-approval")))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("grant-care-plan-approval"))));
        mvc.perform(get("/api/v1/accounts/" + careStaff).header("Authorization", bearer(manager)))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("grant-care-plan-approval"))));
    }

    @Test
    void FR_024_onlyForNurseAccounts() throws Exception {
        long careStaff = fixtures.staffAccount("AC-05");

        commands.account(manager, careStaff, "grant-care-plan-approval", "\"0\"", PermissionCommands.REASON)
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM additional_grant WHERE account_id = ?", Integer.class, careStaff))
                .isZero();
    }

    @Test
    void FR_024_grantCountsOnlyWhileAccountIsNurse() throws Exception {
        long account = fixtures.staffAccount("AC-04", "AC-05");
        String token = fixtures.session(account);
        commands.account(manager, account, "grant-care-plan-approval", "\"0\"", PermissionCommands.REASON)
                .andExpect(status().isOk());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/v1/accounts/" + account + "/remove-role")
                        .header("Authorization", bearer(manager))
                        .header("If-Match", "\"1\"")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"AC-04\",\"reason\":\"Chuyển sang chăm sóc\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(token)))
                .andExpect(jsonPath(CARE_PLAN_APPROVE).isEmpty());
    }
}
