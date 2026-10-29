package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static com.eldercare.backend.administration.account.PermissionCommands.restrict;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 002 T074 (US7 #6, #7; FR-026, BR-M15-04, SC-007): chỉ Quản lý viện đổi, xem quyền của vai trò hay tài khoản (Hành
 * chính có ô {@code ACCOUNT_PERMISSION_PARAMETER} kèm ¹², chỉ cho tài khoản người thân); nhật ký ghi người thực hiện,
 * đối tượng, tập quyền trước, sau và lý do. Tasks ghi {@code PhanQuyenChiQuanLyIT}.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class OnlyManagerChangesPermissionsIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    AccountFixtures fixtures;
    PermissionCommands commands;

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
        commands = new PermissionCommands(mvc, jdbc);
    }

    @AfterEach
    void closeRoleRestrictions() {
        commands.closeRoleRestrictions();
    }

    @Test
    void US7_6_nonManager_cannotChangeOrViewPermissions_403() throws Exception {
        long nurse = fixtures.staffAccount("AC-04");
        for (String role : new String[] {"AC-09", "AC-02", "AC-03"}) {
            String token = fixtures.session(fixtures.staffAccount(role));

            commands.account(token, nurse, "restrict", "\"0\"", restrict("CARE_PLAN", "VIEW", "REMOVE_OPERATION"))
                    .andExpect(status().isForbidden());
            commands.account(token, nurse, "grant-care-plan-approval", "\"0\"", PermissionCommands.REASON)
                    .andExpect(status().isForbidden());
            commands.role(
                            token,
                            "AC-04",
                            "restrict",
                            commands.roleVersion("AC-04"),
                            restrict("CARE_PLAN", "VIEW", "REMOVE_OPERATION"))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/roles/AC-04/permissions").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM permission_restriction WHERE account_id = ?"
                                + " OR (role = 'AC-04' AND effective_to IS NULL)",
                        Integer.class,
                        nurse))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM additional_grant WHERE account_id = ?", Integer.class, nurse))
                .isZero();
    }

    @Test
    void US7_7_audit_actorEntityBeforeAfterReason() throws Exception {
        long managerAccount = fixtures.staffAccount("AC-01");
        String manager = fixtures.session(managerAccount);
        long nurse = fixtures.staffAccount("AC-04");

        commands.account(manager, nurse, "restrict", "\"0\"", restrict("CARE_PLAN", "UPDATE", "REMOVE_OPERATION"))
                .andExpect(status().isOk());
        commands.role(
                        manager,
                        "AC-07",
                        "restrict",
                        commands.roleVersion("AC-07"),
                        restrict("DISH_MENU", "VIEW", "LOWER_SCOPE"))
                .andExpect(status().isOk());

        Map<String, Object> byAccount = jdbc.queryForMap(
                "SELECT actor_id, entity_type, data_before, data_after, reason FROM audit_log"
                        + " WHERE action = 'PERMISSION_RESTRICTED' AND entity_id = ?",
                String.valueOf(nurse));
        assertThat(((Number) byAccount.get("actor_id")).longValue()).isEqualTo(managerAccount);
        assertThat(byAccount.get("entity_type")).isEqualTo("M15.ACCOUNT");
        assertThat(String.valueOf(byAccount.get("data_before"))).contains("CARE_PLAN/UPDATE/ASSIGNED");
        assertThat(String.valueOf(byAccount.get("data_after")))
                .contains("CARE_PLAN/VIEW/ASSIGNED")
                .doesNotContain("CARE_PLAN/UPDATE/ASSIGNED");
        assertThat(byAccount.get("reason")).isEqualTo("Điều chỉnh theo mô hình vận hành");

        Map<String, Object> byRole = jdbc.queryForMap(
                "SELECT entity_type, data_before, data_after FROM audit_log WHERE action = 'PERMISSION_RESTRICTED'"
                        + " AND entity_id = 'AC-07' ORDER BY id DESC LIMIT 1");
        assertThat(byRole.get("entity_type")).isEqualTo("M15.ROLE");
        assertThat(String.valueOf(byRole.get("data_before"))).contains("DISH_MENU/VIEW/FACILITY");
        assertThat(String.valueOf(byRole.get("data_after")))
                .contains("DISH_MENU/VIEW/ASSIGNED")
                .doesNotContain("DISH_MENU/VIEW/FACILITY");
    }
}
