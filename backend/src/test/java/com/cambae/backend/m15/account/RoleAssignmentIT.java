package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeM15Config;
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
 * 002 T033 (US2 #8; FR-003, FR-004, FR-026, FR-027; BR-M15-04, BR-M15-17): Quản lý viện gán, gỡ vai trò nhân viên kèm
 * lý do và {@code If-Match} là {@code permission_version}; quyền mới có hiệu lực ở thao tác kế tiếp; không gỡ vai trò
 * cuối cùng; nhật ký ghi vai trò trước, sau.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class RoleAssignmentIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    AccountFixtures fixtures;
    String manager;

    @BeforeEach
    void managerSignedIn() {
        fixtures = new AccountFixtures(jdbc, time);
        manager = fixtures.session(fixtures.staffAccount("AC-01"));
    }

    @Test
    void US2_8_assignRole_effectiveAtNextRequest_audited() throws Exception {
        long account = fixtures.staffAccount("AC-05");
        String token = fixtures.session(account);
        mvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$[?(@.function == 'CARE_PLAN' && @.operation == 'CREATE')]")
                        .isEmpty());

        command(account, "assign-role", "\"0\"", "{\"role\":\"AC-04\",\"reason\":\"Có chứng chỉ điều dưỡng\"}")
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("AC-04", "AC-05")));

        mvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$[?(@.function == 'CARE_PLAN' && @.operation == 'CREATE')]")
                        .isNotEmpty());
        assertThat(jdbc.queryForObject(
                        "SELECT data_after FROM audit_log WHERE action = 'ROLE_ASSIGNED' AND entity_id = ?",
                        String.class,
                        String.valueOf(account)))
                .contains("AC-04");
    }

    @Test
    void BR_M15_17_staleIfMatch_412() throws Exception {
        long account = fixtures.staffAccount("AC-05");

        command(account, "assign-role", "\"5\"", "{\"role\":\"AC-04\",\"reason\":\"Có chứng chỉ\"}")
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    void FR_004_removeRole_lastRoleRefused_otherRoleRemoved() throws Exception {
        long account = fixtures.staffAccount("AC-04", "AC-05");

        command(account, "remove-role", "\"0\"", "{\"role\":\"AC-04\",\"reason\":\"Hết chứng chỉ\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", containsInAnyOrder("AC-05")));
        command(account, "remove-role", "\"1\"", "{\"role\":\"AC-05\",\"reason\":\"Thử gỡ hết\"}")
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM role_assignment WHERE account_id = ? AND effective_to IS NOT NULL",
                        Integer.class,
                        account))
                .isEqualTo(1);
    }

    /** T076 kéo lên cho màn T035: phần hiện hành của {@code GET /accounts/{id}/permissions} (chưa có {@code asOf}). */
    @Test
    void FR_026a_currentPermissions_etagIsPermissionVersion_onlyManager() throws Exception {
        long account = fixtures.staffAccount("AC-05");

        mvc.perform(get("/api/v1/accounts/" + account + "/permissions").header("Authorization", bearer(manager)))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("AC-05")));
        command(account, "assign-role", "\"0\"", "{\"role\":\"AC-04\",\"reason\":\"Có chứng chỉ điều dưỡng\"}")
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/accounts/" + account + "/permissions").header("Authorization", bearer(manager)))
                .andExpect(header().string("ETag", "\"1\""));

        String office = fixtures.session(fixtures.staffAccount("AC-09"));
        mvc.perform(get("/api/v1/accounts/" + account + "/permissions").header("Authorization", bearer(office)))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_003_assignRoleWithoutReason_422() throws Exception {
        long account = fixtures.staffAccount("AC-05");

        command(account, "assign-role", "\"0\"", "{\"role\":\"AC-04\"}").andExpect(status().isUnprocessableEntity());
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
